package ohnosequences.db.taxonomy

import ohnosequences.s3._
import ohnosequences.forests.{EmptyTree, NonEmptyTree, Tree}, Tree._
import ohnosequences.files.directory.createDirectory
import java.io.File
import ohnosequences.db.ncbitaxonomy.io

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

  /** Performs the mirroring of the tree data
    *
    * For [[data.treeFiles]] for a [[Version]]:
    *
    *   1. Downloads the full tree from S3 if it is not in the local folder
    *   2. Creates the local folders where the objects are going to be stored
    *   3. Generates all the trees (good, environmental and unclassified)
    *   4  Uploads them to S3
    *
    * @param version the [[Version]] we want to mirror
    * @param localFolder the folder where we want to mirror the files
    */
  private def mirrorVersion(
      version: Version,
      localFolder: File
  ): Error + Set[S3Object] = {

    val s3TreeData    = data.treeData(version, TreeType.Full)
    val s3TreeShape   = data.treeShape(version, TreeType.Full)
    val localTreeData = data.local.treeData(version, TreeType.Full, localFolder)
    val localTreeShape =
      data.local.treeData(version, TreeType.Full, localFolder)

    getFileIfDifferent(s3TreeData, localTreeData).flatMap { _ =>
      getFileIfDifferent(s3TreeShape, localTreeShape).flatMap { _ =>
        taxTreeFromFiles(localTreeData, localTreeShape).flatMap { fullTree =>
          // Creates all the folders
          val dirError = TreeType.all.toIterator
            .map { treeType =>
              createDirectory(data.typeFolder(localFolder, treeType))
            }
            .find { createResult =>
              createResult.isLeft
            }

          // There is no
          dirError.fold(
            TreeType.all.toIterator
              .map { treeType =>
                val tree = generateTree(fullTree, treeType)
                val localData =
                  data.local.treeData(version, treeType, localFolder)
                val localShape =
                  data.local.treeShape(version, treeType, localFolder)
                val s3Data  = data.treeData(version, treeType)
                val s3Shape = data.treeShape(version, treeType)

                io.dumpTaxTreeToFiles(tree, localData, localShape)
                  .fold(
                    { err =>
                      Left(Error.FileError(err))
                    }, { _ =>
                      for {
                        _ <- upload(localData, s3Data)
                        _ <- upload(localShape, s3Shape)
                      } yield {
                        Set(s3Data, s3Shape)
                      }
                    }
                  )
              }
              .find { createResult =>
                createResult.isLeft
              }
              .fold(
                Right(TreeType.all.map { data.treeData(version, _) } |
                  TreeType.all
                    .map { data.treeShape(version, _) }): Error + Set[S3Object]
              )(identity)
          )({
            _.left
              .map { err =>
                Error.FileError(err)
              }
              .right
              .map { _ =>
                Set.empty[S3Object]
              }
          })
        }
      }
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
