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
import scala.collection.mutable.{Set => MutableSet}
import java.io.File

case class Node[Payload](
    val payload: Payload,
    val parentPosition: Int, // Index in previous level
    val childrenPositions: Array[Int] // Indices in next level
)

class GenerateTrees extends org.scalatest.FunSuite {
  type TaxNode = Node[Int]

  type Tree =
    Array[Array[TaxNode]]
  // TreePosition = (Array, Position inside array)
  type TreePosition =
    (Int, Int)

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

    val rootChildren  = wholeMap(1)._2
    val root: TaxNode = Node(1, 1, (0 until rootChildren.length).toArray)

    val rootLevel: Array[TaxNode]            = Array(root)
    val initialLevels: Array[Array[TaxNode]] = Array(rootLevel)

    @annotation.tailrec
    def generateTaxTree_rec(
        levels: Array[Array[TaxNode]]): Array[Array[TaxNode]] = {
      val lastLevel = levels.last

      // Compute next level length
      var length = 0
      lastLevel foreach { parent =>
        val (_, childrenIDs) = wholeMap(parent.payload)
        length += childrenIDs.length
      }

      if (length == 0) {
        levels
      } else {
        // Create empty array for next level
        val nextLevel = new Array[TaxNode](length)

        // Populate nextLevel
        var childrenOffset      = 0
        var grandChildrenOffset = 0
        var parentPos           = 0

        // Iterate nodes in current level and add their children to nextLevel
        while (parentPos < lastLevel.length) {
          val parent           = lastLevel(parentPos)
          val (_, childrenIDs) = wholeMap(parent.payload)

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
              Node(childID, parentPos, grandChildrenPositions)

            i += 1
            grandChildrenOffset += grandChildrenNum
          }

          childrenOffset += childrenIDs.length
          parentPos += 1
        }

        generateTaxTree_rec(levels :+ nextLevel)
      }
    }

    generateTaxTree_rec(initialLevels)
  }

  def taxToTreeMap(taxTree: Tree): Map[Int, TreePosition] = {
    val taxtoTreePosition = MutableMap[Int, TreePosition]()
    var levelIndex        = 0
    while (levelIndex < taxTree.length) {
      val level = taxTree(levelIndex)

      var arrayIndex = 0
      while (arrayIndex < level.length) {
        val node = level(arrayIndex)
        taxtoTreePosition += node.payload -> ((levelIndex, arrayIndex))
        arrayIndex += 1
      }

      levelIndex += 1
    }

    taxtoTreePosition.toMap
  }

  // An array from the node level (included) to the root level (included),
  // showing the positions that conform the path from the specified node to the
  // root.
  def lineage(tree: Tree, nodePos: TreePosition): Array[Int] = {
    val (levelIdx, nodeIdx) = nodePos

    val lineage = new Array[Int](levelIdx + 1)
    lineage(levelIdx) = nodeIdx

    var currLevel = levelIdx - 1

    while (currLevel >= 0) {
      val prevLevel = currLevel + 1
      lineage(currLevel) = tree(prevLevel)(lineage(prevLevel)).parentPosition
      currLevel -= 1
    }

    lineage
  }

  def branch(tree: Tree, nodePos: TreePosition): Array[Array[Int]] =
    ???

  def extractSubTree(tree: Tree,
                     predicate: TaxNode => Boolean): Array[Set[Int]] = {
    val selectedNodesByLevel = new Array[MutableSet[Int]](tree.length)

    var levelIdx = 0
    while (levelIdx < tree.length) {
      val level = tree(levelIdx)

      var nodeIdx = 0
      while (nodeIdx < level.length) {
        val node = level(nodeIdx)

        if (predicate(node)) {
          val ancestry = lineage(tree, (levelIdx, nodeIdx))
          val progeny  = branch(tree, (levelIdx, nodeIdx))

          for ((ancestorIdx, ancestryLevelIdx) <- ancestry.view.zipWithIndex) {
            selectedNodesByLevel(ancestryLevelIdx) += ancestorIdx
          }

          for ((progenyLevel, progenyLevelIdx) <- progeny.view.zipWithIndex) {
            selectedNodesByLevel(levelIdx + progenyLevelIdx) ++= progenyLevel.toSet
          }
        }

        nodeIdx += 1
      }

      levelIdx += 1
    }

    selectedNodesByLevel map { _.toSet }
  }

  test("Generate Tree") {
    // Resource preparation
    createDirectoryOrFail(baseDir)

    if (!namesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.names, namesFile)

    if (!nodesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.nodes, nodesFile)

    // Tree generation
    val taxTree = generateTaxTreeFromStrings(
      retrieveLinesFrom(nodesFile).right.getOrElse(fail("Failure"))
    )

    // The number of nodes in ohnosequences.db.ncbitaxonomy.nodes, v0.0.1
    val nodesNumber = 1703606

    // Check the number of nodes in the tree equals the number of nodes in the
    // file
    assert((taxTree map { _.length }).sum == nodesNumber)

    // Check the children positions of each node are legal; i.e., are in the
    // range of the next level array
    for (levelIndex <- 0 until taxTree.length) {
      val level = taxTree(levelIndex)
      level foreach { node =>
        val nextLevelLength =
          if (node.childrenPositions.isEmpty)
            0
          else
            taxTree(levelIndex + 1).length

        node.childrenPositions map { pos =>
          assert(pos < nextLevelLength)
        }
      }
    }

    val idsMap = taxToTreeMap(taxTree)
    println(s"Root lineage   : ${lineage(taxTree, idsMap(1)).mkString(", ")}")
    println(s"Random lineage : ${lineage(taxTree, idsMap(505)).mkString(", ")}")
  }
}
