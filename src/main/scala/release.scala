package ohnosequences.db.taxonomy

import ohnosequences.s3._
import ohnosequences.forests.{EmptyTree, NonEmptyTree, Tree}, Tree._
import ohnosequences.files.directory.createDirectory
import java.io.File

case object release {
  import s3Helpers._

  /** Generates Good, Environmental or Unclassified tree from the Full one
    *
    * @param fullTree the whole [[TaxTree]]
    * @param treeType a type from: [[TreeType.Good]], [[TreeType.Environmental]]
    * or [[TreeType.Unclassified]]
    */
  def generateTree(fullTree: TaxTree, treeType: TreeType): TaxTree =
    fullTree match {
      case fullTree: EmptyTree[TaxNode] => fullTree
      case fullTree: NonEmptyTree[TaxNode] =>
        treeType match {
          case TreeType.Full => fullTree
          case TreeType.Unclassified =>
            val positions = whichPositions(fullTree) { pos =>
              TreeType.get(fullTree(pos)) == TreeType.Unclassified
            }.toSet

            coveringTree(fullTree, positions)
          case TreeType.Environmental =>
            val positions = whichPositions(fullTree) { pos =>
              TreeType.get(fullTree(pos)) == TreeType.Environmental
            }.toSet

            coveringTree(fullTree, positions)
          case TreeType.Good =>
            filter(fullTree) { node =>
              TreeType.get(node) == TreeType.Good
            }
        }
    }

  /** Generates the local directories for a version in a localFolder,
    * that is, the structure:
    *       - `${localFolder}/${version}/Good`
    *       - `${localFolder}/${version}/Environmental`
    *       - `${localFolder}/${version}/Unclassified`
    *
    * @param version the [[Version]] we want to generate
    * @param localFolder indicating the folder whree to store data locally
    * @return a Left(error) if something went wrong with any folder creation,
    * otherswise a Right(files) where files is a collection of the created folders
    */
  private def createDirectories(version: Version,
                                localFolder: File): Error + Set[File] = {
    val dirsToCreate = TreeType.all.map { treeType =>
      data.versionFolder(version, treeType, localFolder)
    }

    dirsToCreate
      .map { dir =>
        createDirectory(dir)
      }
      .find { createResult =>
        createResult.isLeft
      }
      .fold(Right(dirsToCreate): Error + Set[File]) { err =>
        err.left.map(Error.FileError).right.map(_ => Set.empty[File])
      }
  }

  /** Generates the Good, Environmental and Unclassified trees from the full one
    *
    * @param fullTree the Full [[TaxTree]]
    * @param version the [[Version]] we want to generate the trees for
    * @param localFolder indicating the folder whree to store data locally
    * @return a Left(error) if something went wrong with any tree creation, otherwise
    * a Right(result) where result is a collection of pairs `(File, S3Object)` with
    * the generated files plus the S3 address where they should be stored
    */
  private def generateTreesFrom(
      fullTree: TaxTree,
      version: Version,
      localFolder: File): Error + Set[(File, S3Object)] = {

    val localData: TreeType => File =
      data.local.treeData(version, _, localFolder)
    val localShape: TreeType => File =
      data.local.treeShape(version, _, localFolder)
    val s3Data: TreeType => S3Object  = data.treeData(version, _)
    val s3Shape: TreeType => S3Object = data.treeShape(version, _)

    TreeType.exceptFull
      .map { treeType =>
        val tree      = generateTree(fullTree, treeType)
        val dataFile  = localData(treeType)
        val shapeFile = localShape(treeType)

        dumpTaxTreeToFiles(tree, dataFile, shapeFile)
      }
      .find { dumpResult =>
        dumpResult.isLeft
      }
      .fold(
        Right(
          TreeType.exceptFull.map { tType =>
            (localData(tType), s3Data(tType))
          } union
            TreeType.exceptFull.map { tType =>
              (localShape(tType), s3Shape(tType))
            }): Error + Set[(File, S3Object)]
      ) { err =>
        err.map { _ =>
          Set.empty[(File, S3Object)]
        }
      }
  }

  /** Uploads a collection of files to S3
    *
    * @param toUpload a collection of pairs `(File, S3Object)` with the file
    * to upload and its desired S3 destination
    *
    * @return a Left(error) if something went wrong with any tree creation,
    * otherwise a Right(objects) where objects is a collection of the uploaded
    * objects
    */
  private def uploadTrees(
      toUpload: Set[(File, S3Object)]): Error + Set[S3Object] =
    toUpload
      .map {
        case (file, s3Obj) =>
          upload(file, s3Obj)
      }
      .find { uploadResult =>
        uploadResult.isLeft
      }
      .fold(
        Right(toUpload.map { _._2 }): Error + Set[S3Object]
      ) { err =>
        err.map { _ =>
          Set.empty[S3Object]
        }
      }

  /** Performs the mirroring of the tree data for the three following trees:
    * Good, Environmental, Unclassified
    *
    * For a given [[Version]]:
    *
    *   1. Creates the local folders where the objects are going to be stored,
    *      one per `TreeType`:
    *       - `${localFolder}/${version}/good`
    *       - `${localFolder}/${version}/environmental`
    *       - `${localFolder}/${version}/unclassified`
    *       - `${localFolder}/${version}/full
    *   2. Downloads the full tree from S3 if it is not already in the
    *      `localFolder`
    *   3. Reads the full tree
    *   4. Generates all the trees (Good, Environmental and Unclassified)
    *   5  Uploads them to S3
    *
    * @param version the [[Version]] we want to mirror
    * @param localFolder the folder where we want to mirror the files
    *
    * @return Right(objects) where objects is a collection of all the mirrored
    * files (a `data.tree` file and a `shape.tree` file per `TreeType`) if
    * everything went smoothly. Otherwise a Left(error), where error can be due
    * to:
    */
  private def mirrorVersion(
      version: Version,
      localFolder: File
  ): Error + Set[S3Object] = {

    val s3Data     = data.treeData(version, TreeType.Full)
    val s3Shape    = data.treeShape(version, TreeType.Full)
    val localData  = data.local.treeData(version, TreeType.Full, localFolder)
    val localShape = data.local.treeShape(version, TreeType.Full, localFolder)

    for {
      _        <- createDirectories(version, localFolder)
      _        <- getFileIfDifferent(s3Data, localData)
      _        <- getFileIfDifferent(s3Shape, localShape)
      fullTree <- readTaxTreeFromFiles(localData, localShape)
      toUpload <- generateTreesFrom(fullTree, version, localFolder)
      result   <- uploadTrees(toUpload)
    } yield {
      result
    }
  }

  /**
    * Finds any object under [[data.prefix(version)]] that could be overwritten
    * by [[mirrorNewVersion]].
    *
    * @param version is the version that specifies the S3 folder
    *
    * @return Some(object) with the first object found under
    * [[data.prefix(version)]] if any, None otherwise.
    */
  private def findVersionInS3(version: Version): Option[S3Object] =
    data
      .everything(version)
      .find(
        obj => objectExists(obj)
      )

  /** Mirrors a new version of the taxonomy to S3 iff the upload does not
    * overwrite anything.
    *
    * This method tries to download the files for the full taxonomic tree,
    * generate the good, environmental and unclassified trees from it and
    * upload them to S3
    *
    * It does so iff no object (there is a data file and a shape file per
    * tree) exists for any of the trees, the local folder can be created,
    * the full tree files are equal to the remote files / can be successfully
    * downloaded and everything goes smoothly with each of the taxonomic trees
    * creation
    *
    * @param version is the new version that wants to be released
    * @param localFolder is the localFolder where the downloaded files will be
    * stored.
    *
    * @return an Error + Set[S3Object], with a Right(set) with all the mirrored
    * S3 objects if everything worked as expected or with a Left(error) if an
    * error occurred.
    */
  def mirrorNewVersion(
      version: Version,
      localFolder: File
  ): Error + Set[S3Object] =
    findVersionInS3(version).fold(
      mirrorVersion(version, localFolder)
    ) { obj =>
      Left(Error.S3ObjectExists(obj))
    }

}
