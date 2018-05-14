package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.test.utils.{
  createDirectory,
  downloadFrom,
  retrieveLinesFrom
}
import ohnosequences.db.taxonomy._
import ohnosequences.api.ncbitaxonomy.{Rank, TaxID, TreeMap, io}
import ohnosequences.test.ReleaseOnlyTest
import ohnosequences.awstools.s3.S3Object
import ohnosequences.trees.{Index, Node, NodePosition, Tree, TreeIndices}
import java.io.File

class GenerateTrees extends org.scalatest.FunSuite {
  type TaxNode = Node[TaxID]
  type TaxTree = Tree[TaxID]

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

  def treeMapToTaxTree(wholeMap: TreeMap, rootID: TaxID): TaxTree = {
    val rootChildren = wholeMap(rootID)._2
    val root: TaxNode =
      new TaxNode(rootID, None, 0, rootChildren.length)

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

            nextLevel(childrenOffset + i) = new Node(childID,
                                                     Some(parentPos),
                                                     grandChildrenOffset,
                                                     grandChildrenNum)

            i += 1
            grandChildrenOffset += grandChildrenNum
          }

          childrenOffset += childrenIDs.length
          parentPos += 1
        }

        generateTaxTree_rec(levels :+ nextLevel)
      }
    }

    new TaxTree(generateTaxTree_rec(initialLevels))
  }

  def taxSubTreeIndicesToTreeMap(taxTree: TaxTree,
                                 positions: Array[NodePosition]): TreeMap =
    positions.foldLeft(TreeMap()) {
      case (map, nodePos) =>
        val node = taxTree(nodePos).payload
        val parent = taxTree.parent(nodePos) map { parentPos =>
          taxTree(parentPos).payload
        }
        val children = taxTree.children(nodePos) map { childPos =>
          taxTree(childPos).payload
        }

        map + (node -> ((parent, children)))
    }

  test("Generate trees", ReleaseOnlyTest) {
    createDirectoryOrFail(baseDir)

    if (!namesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.names, namesFile)

    if (!nodesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.nodes, nodesFile)

    val nodesIt = retrieveLinesFrom(nodesFile).right.getOrElse(fail("Failure"))
    val namesIt = retrieveLinesFrom(namesFile).right.getOrElse(fail("Failure"))

    // Generate tree map with the taxonomic root we need, to force that the only
    // considered nodes ara Archaea and Bacteria
    val treeMap: TreeMap = io.generateNodesMap(nodesIt) + data.root

    // Auxiliary information: names and ranks
    val namesMap: Map[TaxID, String] = io.generateNamesMap(namesIt)

    // Convert to TaxTree
    val fullTree: TaxTree = treeMapToTaxTree(treeMap, data.rootID)

    val envTree: Array[NodePosition] = fullTree.subTreeIndices({ node =>
      api.NameType.isEnvironmental(namesMap(node.payload))
    })

    val uncTree: Array[NodePosition] = fullTree.subTreeIndices({ node =>
      api.NameType.isUnclassified(namesMap(node.payload))
    })

    val claTree: TaxTree = fullTree.subTreePruningAll({ node =>
      !api.NameType.isClassified(namesMap(node.payload))
    })

    val envMap: TreeMap = taxSubTreeIndicesToTreeMap(fullTree, envTree)
    val uncMap: TreeMap = taxSubTreeIndicesToTreeMap(fullTree, uncTree)
    // val claMap: TreeMap = taxSubTreeIndicesToTreeMap(fullTree, claTree)
  }

  // test("Tree length") {
  //
  //   // The number of nodes in ohnosequences.db.ncbitaxonomy.nodes, v0.0.1
  //   val nodesNumber = 1703606
  //
  //   // Check the number of nodes in the tree equals the number of nodes in the
  //   // file
  //   assert((taxTree.tree map { _.length }).sum == nodesNumber)
  // }
  //
  // test("Node positions") {
  //
  //   // Check the children positions of each node are legal; i.e., are in the
  //   // range of the next level array
  //   for (levelIndex <- 0 until taxTree.depth) {
  //     val level = taxTree.level(levelIndex)
  //     level foreach { node =>
  //       val nextLevelLength =
  //         if (node.childrenIndices.isEmpty)
  //           0
  //         else
  //           taxTree.level(levelIndex + 1).length
  //
  //       node.childrenIndices map { pos =>
  //         assert(pos < nextLevelLength)
  //       }
  //     }
  //   }
  // }
  //
  // val rootID: TaxID = 1
  // val aTaxID: TaxID = 505
  //
  // val rootIDPos: NodePosition = taxTree.pos(rootID)
  // val aTaxIDPos: NodePosition = taxTree.pos(aTaxID)
  //
  // test("Lineages") {
  //   println(s"Root lineage : ${taxTree.lineage(rootIDPos).mkString(", ")}")
  //   println(s"Random lineage : ${taxTree.lineage(aTaxIDPos).mkString(", ")}")
  // }
  //
  // test("Branches") {
  //   println(
  //     s"Random branch with ${taxTree.branch(aTaxIDPos).length} levels : " +
  //       taxTree
  //         .branch(aTaxIDPos)
  //         .map({ l =>
  //           l.mkString(", ")
  //         })
  //         .mkString("; ")
  //   )
  // }
  //
  // test("Extract subtree") {
  //   val subTree505 = taxTree.extractSubTree({ node =>
  //     node.payload == aTaxID
  //   })
  //
  //   val lineage505 = taxTree.lineage(aTaxIDPos)
  //   val branch505  = taxTree.branch(aTaxIDPos)
  //
  //   val (levelIdx, _) = aTaxIDPos
  //
  //   for (i <- 0 until branch505.length) {
  //     branch505(i).toSet == subTree505(levelIdx + i)
  //   }
  //
  //   for (i <- 0 until lineage505.length) {
  //     Set(lineage505(i)) == subTree505(i)
  //   }
  // }
  //
  // test("Unclassified tree") {
  //   import ohnosequences.db.taxonomy.api.NameType
  //
  //   val unclassifiedTree = taxTree.extractSubTree({ node =>
  //     api.NameType.get(nodesNames(node.payload)) == api.NameType.Unclassified
  //   })
  //
  //   // Check that every node in the subtree has either an ancestor or a
  //   // descendant that is unclassified
  //   for ((level, depth) <- unclassifiedTree.view.zipWithIndex) {
  //     level map { nodeIdx =>
  //       val nodePos: NodePosition = (depth, nodeIdx)
  //       val ancestorsIndices      = taxTree.lineage(nodePos)
  //       val descendantsIndices    = taxTree.branch(nodePos)
  //
  //       // Check if any node in lineage is unclassified
  //       val anc: Seq[Boolean] =
  //         ancestorsIndices.view.zipWithIndex.map {
  //           case (nodeIdx, ancDepth) =>
  //             val sciName = NameType.get(
  //               nodesNames(taxTree((ancDepth, nodeIdx)).payload)
  //             )
  //             sciName == NameType.Unclassified
  //         }
  //
  //       // TODO Check if any node in subtree is unclassified
  //       val des: Seq[Boolean] =
  //         descendantsIndices.view.zipWithIndex.flatMap {
  //           case (descLevel, descDepth) =>
  //             descLevel map { nodeIdx =>
  //               val sciName = NameType.get(
  //                 nodesNames(taxTree((descDepth + depth, nodeIdx)).payload)
  //               )
  //               sciName == NameType.Unclassified
  //             }
  //         }
  //
  //       assert { anc.contains(true) || des.contains(true) }
  //     }
  //   }
  // }
  //
  // test("LCA of all nodes in the most populated (8th at the moment) level") {
  //   taxTree.lca(
  //     Array.tabulate(taxTree.level(8).length) { i =>
  //       (8, i)
  //     }
  //   )
  // }
  //
  // test("LCA of a deep node repeated many times") {
  //   val firstDeepestNode = (lastIndex, 0)
  //   taxTree.lca(Array.tabulate(100000) { i =>
  //     firstDeepestNode
  //   })
  // }
  //
  // test("LCA of all nodes in the tree") {
  //   taxTree.lca(taxTree.indices.toArray)
  // }
}
