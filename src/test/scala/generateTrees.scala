package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy._
import ohnosequences.api.ncbitaxonomy.{Rank, TaxID, TaxTree, TreeMap, io}
import ohnosequences.test.ReleaseOnlyTest
import ohnosequences.trees.io.{fromCSV}
import java.io.File

class GenerateTrees extends IOSuite {
  test("Generate trees", ReleaseOnlyTest) {
    createDirectoryOrFail(baseDir)

    if (!namesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.names, namesFile)

    if (!nodesFile.exists)
      downloadOrFail(ohnosequences.db.ncbitaxonomy.nodes, nodesFile)

    val nodesIt = retrieveLinesFromOrFail(nodesFile)
    val namesIt = retrieveLinesFromOrFail(namesFile)

    // Generate tree map and auxiliary information: names and ranks
    val treeMap: TreeMap             = io.generateNodesMap(nodesIt)
    val namesMap: Map[TaxID, String] = io.generateNamesMap(namesIt)
    val ranksMap: Map[TaxID, Rank]   = io.generateRanksMap(namesIt)

    // Convert to TaxTree
    val fullTree: TaxTree = io.treeMapToTaxTree(treeMap, data.rootID)

    // Extract environmental subtree positions
    val envTree: TaxTree = fullTree.subTree({ node =>
      api.NameType.isEnvironmental(namesMap(node.payload))
    })

    // Extract unclassified subtree positions
    val uncTree: TaxTree = fullTree.subTree({ node =>
      api.NameType.isUnclassified(namesMap(node.payload))
    })

    // Build classified tree
    val claTree: TaxTree = fullTree.pruneAll({ node =>
      !api.NameType.isClassified(namesMap(node.payload))
    })

    def taxIDToString: TaxID => String = _.toString

    val envIterator  = toCSVOrFail(envTree, taxIDToString)
    val uncIterator  = toCSVOrFail(uncTree, taxIDToString)
    val claIterator  = toCSVOrFail(claTree, taxIDToString)
    val fullIterator = toCSVOrFail(fullTree, taxIDToString)

    linesToFileOrFail(envFile)(envIterator)
    linesToFileOrFail(uncFile)(uncIterator)
    linesToFileOrFail(claFile)(claIterator)
    linesToFileOrFail(fullFile)(fullIterator)

    uploadToOrFail(envFile, s3.environmentalTree)
    uploadToOrFail(uncFile, s3.unclassifiedTree)
    uploadToOrFail(claFile, s3.classifiedTree)
    uploadToOrFail(fullFile, s3.fullTree)
  }

  test("Read trees", ReleaseOnlyTest) {
    def stringToTaxID: String => TaxID = _.toInt

    val fullLines = retrieveLinesFromOrFail(fullFile)
    val envLines  = retrieveLinesFromOrFail(envFile)
    val uncLines  = retrieveLinesFromOrFail(uncFile)
    val claLines  = retrieveLinesFromOrFail(claFile)

    assert { fromCSV(envLines, stringToTaxID).isRight }
    assert { fromCSV(uncLines, stringToTaxID).isRight }
    assert { fromCSV(claLines, stringToTaxID).isRight }
    assert { fromCSV(fullLines, stringToTaxID).isRight }
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
