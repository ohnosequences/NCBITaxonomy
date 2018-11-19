package ohnosequences.db.taxonomy

import ohnosequences.s3._
import ohnosequences.forests.{EmptyTree, NonEmptyTree, Tree}, Tree._
import java.io.File
import helpers._

case object release {

  /** Generates Good, Environmental or Unclassified tree from the Full one
    *
    * @param fullTree the whole [[TaxTree]]
    * @param treeType a type from: [[TreeType.Good]], [[TreeType.Environmental]]
    * or [[TreeType.Unclassified]]
    */
  private def generateTree(fullTree: TaxTree, treeType: TreeType): TaxTree =
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

  /** Generates the Good, Environmental and Unclassified trees from the full one
    *
    * @param fullTree the Full [[TaxTree]]
    * @param version the [[Version]] we want to generate the trees for
    * @return a Left(error) if something went wrong with any tree creation, otherwise
    * a Right(result) where result is a collection of pairs `(File, S3Object)` with
    * the generated files plus the S3 address where they should be stored
    */
  private def generateTreesFrom(
      fullTree: TaxTree,
      version: Version): Error + Set[(File, S3Object)] = {

    val localData: TreeType => File =
      data.local.treeData(version, _)
    val localShape: TreeType => File =
      data.local.treeShape(version, _)
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
    *
    * @return Right(objects) where objects is a collection of all the mirrored
    * files (a `data.tree` file and a `shape.tree` file per `TreeType`) if
    * everything went smoothly. Otherwise a Left(error), where error can be due
    * to:
    *
    *   - An error creating the directory structure
    *   - An error comparing the local tree files with the S3 ones or downloading
    *     the tree from S3 if it is not stored locally
    *   - An error reading the tree from the local files
    *   - An error uploading the Good, Environmental or Unclassified trees to S3
    *
    */
  private def mirrorVersion(version: Version): Error + Set[S3Object] = {
    val readTree = (readTaxTreeFromFiles _).tupled

    for {
      folders    <- createDirectories(version)
      localFiles <- downloadTreeIfNotInLocalFolder(version, TreeType.Full)
      fullTree   <- readTree(localFiles)
      toUpload   <- generateTreesFrom(fullTree, version)
      result     <- uploadTrees(toUpload)
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
    * tree) exists for any of the trees, the [[data.localFolder]] can be created,
    * the full tree files are equal to the remote files / can be successfully
    * downloaded and everything goes smoothly with each of the taxonomic trees
    * creation
    *
    * @param version is the new version that wants to be released
    *
    * @return an Error + Set[S3Object], with a Right(set) with all the mirrored
    * S3 objects if everything worked as expected or with a Left(error) if an
    * error occurred.
    */
  def mirrorNewVersion(version: Version): Error + Set[S3Object] =
    findVersionInS3(version).fold(
      mirrorVersion(version)
    ) { obj =>
      Left(Error.S3ObjectExists(obj))
    }

}
