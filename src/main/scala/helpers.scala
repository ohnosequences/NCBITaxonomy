package ohnosequences.db.taxonomy

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import ohnosequences.s3._
import ohnosequences.files.directory.createDirectory
import java.io.File

/**
  * Partial applications of functions from `s3`, using a standard S3Client
  * built here, [[s3Helpers.s3Client]], and with a default part size,
  * [[s3Helpers.partSize5MiB]].
  */
private[taxonomy] case object helpers {

  lazy val s3Client = AmazonS3ClientBuilder.standard().build()

  val partSize5MiB = 5 * 1024 * 1024

  /** Downloads the specified `s3Obj` to a give `file` */
  def download(s3Obj: S3Object, file: File) =
    request
      .getCheckedFile(s3Client)(s3Obj, file)
      .left
      .map { err =>
        Error.S3Error(err)
      }

  /** Uploads the specified `file` a given `s3Obj` */
  def upload(file: File, s3Obj: S3Object) =
    request
      .paranoidPutFile(s3Client)(file, s3Obj, partSize5MiB)(
        data.hashingFunction
      )
      .left
      .map { err =>
        Error.S3Error(err)
      }

  /** Downloads the `s3Obj` to `file` whenever `file` does not exist
    * or its checksum is different from the `s3Obj` checksum
    */
  def getFileIfDifferent(s3Obj: S3Object, file: File) =
    request
      .getCheckedFileIfDifferent(s3Client)(s3Obj, file)
      .left
      .map { err =>
        Error.S3Error(err)
      }

  /** Returns true when object does not exists or communication with S3
    * cannot be established */
  def objectExists(s3Obj: S3Object) =
    request
      .objectExists(s3Client)(s3Obj)
      .fold(
        err => true,
        identity
      )

  /** Downloads the shape and data files to [[data.localFolder]] corresponding
    * to the appropriate version for a given [[TreeType]], whenever they are
    * not already stored locally
    */
  def downloadTreeIfNotInLocalFolder(
      version: Version,
      treeType: TreeType): Error + (File, File) = {
    val s3Data     = data.treeData(version, treeType)
    val s3Shape    = data.treeShape(version, treeType)
    val localData  = data.local.treeData(version, treeType)
    val localShape = data.local.treeShape(version, treeType)

    for {
      dataFile  <- getFileIfDifferent(s3Data, localData)
      shapeFile <- getFileIfDifferent(s3Shape, localShape)
    } yield {
      (dataFile, shapeFile)
    }
  }

  /** Generates the local directories for a version in [[data.localFolder]]
    * that is, the structure:
    *       - `${localFolder}/${version}/Good`
    *       - `${localFolder}/${version}/Environmental`
    *       - `${localFolder}/${version}/Unclassified`
    *
    * @param version the [[Version]] we want to generate
    * @return a Left(error) if something went wrong with any folder creation,
    * otherwise a Right(files) where files is a collection of the created folders
    */
  def createDirectories(version: Version): Error + Set[File] = {
    val dirsToCreate = TreeType.all.map { treeType =>
      data.versionFolder(version, treeType)
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
}
