package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.test.utils.{createDirectory, downloadFrom}
import ohnosequences.db.taxonomy._
import ohnosequences.test.ReleaseOnlyTest
import ohnosequences.awstools.s3.S3Object
import java.io.File

class GenerateTrees extends org.scalatest.FunSuite {

  def getOrFail[E <: Error, X]: E + X => X =
    _ match {
      case Right(x) => x
      case Left(e)  => fail(e.msg)
    }

  def downloadOrFail(s3Object: S3Object, file: File) =
    getOrFail {
      downloadFrom(s3Object, file)
    }

  def createDirectoryOrFail(dir: File) =
    getOrFail {
      createDirectory(dir)
    }

  def generateTrees(names: File, nodes: File) =
    ???

  test("Generate Tree", ReleaseOnlyTest) {
    // Resource preparation
    createDirectoryOrFail(baseDir)
    downloadOrFail(ohnosequences.db.ncbitaxonomy.names, namesFile)
    downloadOrFail(ohnosequences.db.ncbitaxonomy.nodes, nodesFile)

    // Tree generation
    generateTrees(namesFile, nodesFile)
  }
}
