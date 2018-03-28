package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.+
import ohnosequences.awstools.s3, s3.S3Object
import java.io.File
import com.amazonaws.services.s3.transfer.TransferManagerBuilder

sealed trait Error {
  val msg: String
}

case object Error {
  final case class Download(val msg: String)     extends Error
  final case class DirCreation(val msg: String)  extends Error
  final case class FileNotFound(val msg: String) extends Error
}

case object utils {

  /**
    * Returns `Right(file)` if the download from `s3Object` to `file`
    * succeeded, `Left(Error.Download(msg))` otherwise.
    */
  def downloadFrom(s3Object: S3Object, file: File): Error.Download + File = {
    println(s"Downloading $s3Object to $file.")
    val tm = TransferManagerBuilder
      .standard()
      .withS3Client(s3.defaultClient)
      .build()

    scala.util.Try {
      tm.download(
          s3Object.bucket,
          s3Object.key,
          file
        )
        .waitForCompletion()
    } match {
      case scala.util.Success(s) => Right(file)
      case scala.util.Failure(e) => Left(Error.Download(e.toString))
    }
  }

  /**
    * Returns `Right(directory)` if it was possible to create all directories
    * in `directory` (or if they already existed);
    * `Left(Error.DirCreation(msg))` otherwise.
    */
  def createDirectory(directory: File): Error.DirCreation + File =
    if (!directory.exists)
      if (directory.mkdirs())
        Right(directory)
      else
        Left(Error.DirCreation(s"Error creating directory $directory."))
    else
      Right(directory)

  /**
    * Returns `Right(Iterator[String])` if it was possible to read the lines from the file, `Left(Error.FileNotFound(msg))` otherwise.
    */
  def retrieveLinesFrom(file: File): Error.FileNotFound + Iterator[String] =
    if (file.exists)
      Right(io.Source.fromFile(file.getCanonicalPath).getLines)
    else
      Left(Error.FileNotFound(s"Error reading $file: file does not exist."))
}
