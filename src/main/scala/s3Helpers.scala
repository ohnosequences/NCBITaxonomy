package ohnosequences.db.taxonomy

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import ohnosequences.s3._
import java.io.File

/**
  * Partial applications of functions from `s3`, using a standard S3Client
  * built here, [[s3Helpers.s3Client]], and with a default part size,
  * [[s3Helpers.partSize5MiB]].
  */
private[taxonomy] case object s3Helpers {

  lazy val s3Client = AmazonS3ClientBuilder.standard().build()
  val partSize5MiB  = 5 * 1024 * 1024

  def download(s3Obj: S3Object, file: File) =
    request
      .getCheckedFile(s3Client)(s3Obj, file)
      .left
      .map { err =>
        Error.S3Error(err)
      }

  def upload(file: File, s3Obj: S3Object) =
    request
      .paranoidPutFile(s3Client)(file, s3Obj, partSize5MiB)(
        data.hashingFunction
      )
      .left
      .map { err =>
        Error.S3Error(err)
      }

  def getFileIfDifferent(s3Obj: S3Object, file: File) =
    request
      .getCheckedFileIfDifferent(s3Client)(s3Obj, file)
      .left
      .map { err =>
        Error.S3Error(err)
      }

  def objectExists(s3Obj: S3Object) =
    request
      .objectExists(s3Client)(s3Obj)
      .fold(
        err => true,
        identity
      )
}
