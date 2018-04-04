package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.test.utils.{
  createDirectory,
  downloadFrom,
  retrieveLinesFrom
}
import ohnosequences.db.taxonomy._
import ohnosequences.api.ncbitaxonomy.dmp
import ohnosequences.test.ReleaseOnlyTest
import ohnosequences.awstools.s3.S3Object
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.{ArrayBuffer => MutableArrayBuffer}
import java.io.File

case class TaxNode(
    val taxID: Int,
    val parentPosition: Int
    // val childrenPositions: Array[Int]
)

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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def generateTrees(names: File, nodes: File) = {
    // Create a map TaxID -> ParentID
    val parentsMap: Map[Int, Int] =
      retrieveLinesFrom(nodes).right map { iterator =>
        dmp.nodes
          .fromLines(iterator)
          .map({ node =>
            node.ID.toInt -> node.parentID.toInt
          })
          .toMap
      } getOrElse (fail("Failure"))

    // Create a map TaxID -> List[ChildID]
    val childrenMap: MutableMap[Int, MutableArrayBuffer[Int]] =
      parentsMap.foldLeft(MutableMap[Int, MutableArrayBuffer[Int]]()) {
        case (currentMap, (nodeID, parentID)) =>
          if (currentMap.isDefinedAt(parentID)) {
            currentMap(parentID) += nodeID
          } else {
            currentMap += (parentID -> MutableArrayBuffer(nodeID))
          }
          currentMap
      }

    // Create a map TaxID -> (ParentID, List[ChildID])
    val wholeMap: Map[Int, (Int, Array[Int])] = parentsMap
      .foldLeft(Map[Int, (Int, Array[Int])]()) {
        case (currentMap, (nodeID, parentID)) =>
          val children =
            childrenMap.get(nodeID).fold(Array[Int]()) { _.toArray }
          val value = (parentID, children)
          currentMap + (nodeID -> value)
      }

    val root: TaxNode                        = TaxNode(1, 1)
    val rootLevel: Array[TaxNode]            = Array(root)
    val initialLevels: Array[Array[TaxNode]] = Array(rootLevel)

    var levelCount = 0

    @annotation.tailrec
    def foo(levels: Array[Array[TaxNode]]): Array[Array[TaxNode]] = {
      println(s"Level $levelCount.")
      levelCount += 1

      if (levels.last.isEmpty)
        levels
      else {
        // Compute next level length
        var length = 0
        levels.last foreach { parent =>
          length += wholeMap(parent.taxID)._2.length
        }
        println(s"Next level length = $length")

        // Create empty array for next level
        val nextLevel = new Array[TaxNode](length)

        // Populate nextLevel
        var offset    = 0
        var parentPos = 0
        // Iterate nodes in current level and add their children to nextLevel
        while (parentPos < levels.last.length) {
          val parent      = levels.last(parentPos)
          val childrenIDs = wholeMap(parent.taxID)._2

          var i = 0
          while (i < childrenIDs.length) {
            nextLevel(offset + i) = TaxNode(childrenIDs(i), parent.taxID)
            i += 1
          }

          offset += childrenIDs.length
          parentPos += 1
        }

        foo(levels :+ nextLevel)
      }
    }

    foo(initialLevels)
  }

  test("Generate Tree") {
    // Resource preparation
    // createDirectoryOrFail(baseDir)
    // downloadOrFail(ohnosequences.db.ncbitaxonomy.names, namesFile)
    // downloadOrFail(ohnosequences.db.ncbitaxonomy.nodes, nodesFile)

    // Tree generation
    generateTrees(namesFile, nodesFile)
  }
}
