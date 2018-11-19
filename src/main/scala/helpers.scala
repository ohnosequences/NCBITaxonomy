package ohnosequences.db.taxonomy

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import ohnosequences.s3._
import ohnosequences.files.directory.createDirectory
import ohnosequences.db.ncbitaxonomy.io
import java.io.File

/**
  * Helpers:
  * - Partial applications of functions from `s3`, using a standard S3Client
  *   built here, [[s3Helpers.s3Client]], and with a default part size,
  *   [[s3Helpers.partSize5MiB]].
  * - Method to create directories or fail if some creation went wrong
  * - Methods to read / dump trees from / to files.
  * - Method to upload a bunch of files to S3, or fail if some upload went
  *   wrong
  * - Method to check if all the objects for a version are in S3
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

  /** Reads taxonomic tree serialized into a `data` file and a `shape` file
    *
    * @param data the file for the tree data
    * @param shape the file with the structure of nodes of the tree
    *
    * @return a Left(error) if some error arised reading the files (e.g. one
    * or both of them do not exist), a Right(tree) otherwise
    */
  def readTaxTreeFromFiles(data: File, shape: File): Error + TaxTree =
    io.readTaxTreeFromFiles(data, shape)
      .left
      .map { err =>
        err.fold(Error.FileError, Error.SerializationError)
      }

  /** Dumps serialization for a taxonomic tree into a `data` file and a
    * `shape` file
    *
    * @param tree a taxonomic tree
    * @param data the file where we want to dump the tree data
    * @param shape the file with the structure of nodes of the tree
    *
    * @return a Left(error) if some error arised writing to the files,
    * a Right(files) where files is a tuple of files (data, shape) otherwise
    */
  def dumpTaxTreeToFiles(tree: TaxTree,
                         data: File,
                         shape: File): Error + (File, File) =
    io.dumpTaxTreeToFiles(tree, data, shape)
      .left
      .map(Error.FileError)

  /**
    * Finds any object under [[data.prefix(version)]] that could be overwritten
    * by [[mirrorNewVersion]].
    *
    * @param version is the version that specifies the S3 folder
    *
    * @return Some(object) with the first object found under
    * [[data.prefix(version)]] if any, None otherwise.
    */
  def findVersionInS3(version: Version): Option[S3Object] =
    data
      .everything(version)
      .find(
        obj => objectExists(obj)
      )

  /** Uploads a collection of files to S3
    *
    * @param toUpload a collection of pairs `(File, S3Object)` with the file
    * to upload and its desired S3 destination
    *
    * @return a Left(error) if something went wrong with the upload of any file,
    * otherwise a Right(objects) where objects is a collection of the uploaded
    * objects
    */
  def uploadFiles(toUpload: Set[(File, S3Object)]): Error + Set[S3Object] =
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

}
