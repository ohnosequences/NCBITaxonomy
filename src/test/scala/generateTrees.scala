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
    val parentPosition: Int, // Index in previous level
    val childrenPositions: Array[Int] // Indices in next level
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

  // Return a map TaxID -> (ParentID, List[ChildID])
  def generateNodesMap(lines: Iterator[String]) = {
    // Create a map TaxID -> ParentID
    val parentsMap: Map[Int, Int] =
      dmp.nodes
        .fromLines(lines)
        .map({ node =>
          if (node.ID == node.parentID)
            node.ID.toInt -> -1
          else
            node.ID.toInt -> node.parentID.toInt
        })
        .toMap

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
            childrenMap.get(nodeID).map({ _.toArray }).getOrElse(Array[Int]())
          val value = (parentID, children)
          currentMap + (nodeID -> value)
      }

    // Return whole map
    wholeMap
  }

  def generateTaxTreeFromStrings(nodesLines: Iterator[String]) = {
    val wholeMap = generateNodesMap(nodesLines)

    val rootChildren = wholeMap(1)._2
    val root         = TaxNode(1, 1, (0 until rootChildren.length).toArray)

    val rootLevel: Array[TaxNode]            = Array(root)
    val initialLevels: Array[Array[TaxNode]] = Array(rootLevel)

    @annotation.tailrec
    def foo(levels: Array[Array[TaxNode]]): Array[Array[TaxNode]] = {
      val lastLevel = levels.last

      if (lastLevel.isEmpty)
        levels
      else {
        // Compute next level length
        var length = 0
        lastLevel foreach { parent =>
          val (_, childrenIDs) = wholeMap(parent.taxID)
          length += childrenIDs.length
        }

        // Create empty array for next level
        val nextLevel = new Array[TaxNode](length)

        // Populate nextLevel
        var childrenOffset      = 0
        var grandChildrenOffset = 0
        var parentPos           = 0
        // Iterate nodes in current level and add their children to nextLevel
        while (parentPos < lastLevel.length) {
          val parent           = lastLevel(parentPos)
          val (_, childrenIDs) = wholeMap(parent.taxID)

          var i = 0
          while (i < childrenIDs.length) {
            val childID               = childrenIDs(i)
            val (_, grandChildrenIDs) = wholeMap(childID)
            val grandChildrenNum      = grandChildrenIDs.length
            val grandChildrenPositions =
              Array.tabulate(grandChildrenNum) { i =>
                grandChildrenOffset + i
              }

            nextLevel(childrenOffset + i) =
              TaxNode(childID, parentPos, grandChildrenPositions)

            i += 1
            grandChildrenOffset += grandChildrenNum
          }

          childrenOffset += childrenIDs.length
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
