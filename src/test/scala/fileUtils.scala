package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.+
import ohnosequences.api.ncbitaxonomy.{TaxID, TaxTree}
import ohnosequences.trees
import ohnosequences.awstools.s3, s3.S3Object
import java.io.File
import com.amazonaws.services.s3.transfer.TransferManagerBuilder

sealed trait Error {
  val msg: String
}

case object Error {
  final case class Download(val msg: String)     extends Error
  final case class Upload(val msg: String)       extends Error
  final case class DirCreation(val msg: String)  extends Error
  final case class FileNotFound(val msg: String) extends Error
  final case class WriteFailure(val msg: String) extends Error
  final case class ToCSVFailure(val msg: String) extends Error
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
    * Returns `Right(Iterator[String])` if it was possible to read the lines
    * from the file, `Left(Error.FileNotFound(msg))` otherwise.
    */
  def retrieveLinesFrom(file: File): Error.FileNotFound + Iterator[String] =
    if (file.exists)
      Right(io.Source.fromFile(file.getCanonicalPath).getLines)
    else
      Left(Error.FileNotFound(s"Error reading $file: file does not exist."))

  def printToFile(file: File)(
      op: java.io.PrintWriter => Unit
  ): Error.WriteFailure + File = {
    val p = new java.io.PrintWriter(file)
    try {
      op(p)
      Right(file)
    } catch {
      case e: Throwable =>
        Left(
          Error.WriteFailure(
            s"Exception raised when trying to write to file $file: $e"
          ))
    } finally {
      p.close()
    }
  }

  def linesToFile(file: File)(
      lines: Iterator[String]
  ): Error.WriteFailure + File =
    printToFile(file) { p =>
      lines.foreach(p.println)
    }

  /**
    * Returns `Right(s3Object)` if the upload from `file` to `s3Object`
    * succeeded, ``Left(Error.Upload(msg))`` otherwise.
    */
  def uploadTo(file: File, s3Object: S3Object): Error.Upload + S3Object =
    scala.util.Try {
      s3.defaultClient.putObject(
        s3Object.bucket,
        s3Object.key,
        file
      )
    } match {
      case scala.util.Success(s) =>
        Right(s3Object)
      case scala.util.Failure(e) =>
        Left(Error.Upload(s"Error uploading$file to $s3Object: ${e.toString}."))
    }

}

abstract class IOSuite extends org.scalatest.FunSuite {
  def getOrFail[E <: Error, X]: E + X => X =
    _ match {
      case Right(x) => x
      case Left(e)  => fail(e.msg)
    }

  def downloadOrFail(s3Object: S3Object, file: File) =
    getOrFail {
      utils.downloadFrom(s3Object, file)
    }

  def createDirectoryOrFail(dir: File) =
    getOrFail {
      utils.createDirectory(dir)
    }

  def retrieveLinesFromOrFail(file: File) =
    getOrFail {
      utils.retrieveLinesFrom(file)
    }

  def linesToFileOrFail(file: File)(lines: Iterator[String]) =
    getOrFail {
      utils.linesToFile(file)(lines)
    }

  def uploadToOrFail(file: File, s3Object: S3Object) =
    getOrFail {
      utils.uploadTo(file, s3Object)
    }

  def toCSVOrFail(tree: TaxTree, name: TaxID => String) =
    getOrFail {
      trees.io.toCSV(tree, name).left.map { err =>
        Error.ToCSVFailure(s"Error writing line to CSV: $err")
      }
    }
}
