package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy, taxonomy.{TaxNode, TreeType, Version}
import ohnosequences.db.ncbitaxonomy.Rank
import ohnosequences.forests._
import org.scalatest.EitherValues._

class RankHierarchy extends FunSuite {

  /**
    * Returns, if any, the closest ancestor of a node in a tree whose rank is
    * different than NoRank
    *
    * @param tree is the tree where the ancestor will be searched
    * @param initialPos is the position of the node whose ancestors will be
    * queried
    *
    * @return an `Option[TaxNode]`, `None` if no ancestor with rank was found or
    * `Some(node)` with the first ancestor with rank if it exists.
    */
  def closestAncestorWithRank(
      tree: NonEmptyTree[TaxNode],
      initialPos: TreePosition
  ): Option[TaxNode] = {
    @annotation.tailrec
    def recUntilRoot(pos: TreePosition): Option[TaxNode] =
      pos match {
        case pos: TreePosition.Root => None
        case pos: TreePosition.Children =>
          if (tree(pos).rank != Rank.NoRank)
            Some(tree(pos))
          else
            recUntilRoot(Tree.parentPosition(tree, pos))
      }

    recUntilRoot(Tree.parentPosition(tree, initialPos))
  }

  /**
    * Checks whether all trees respect the Rank hierarchy; i.e., the ancestors
    * of every node have ranks less specific (or equal) of the node's rank,
    * skipping all nodes with rank NoRank.
    *
    * @note we accept nodes whose ancestors have equal rank, as it does happen
    * in the taxonomy.
    */
  ignore(
    "All versions trees respect the Rank hierarchy - ignored because the hierarchy is not strictly preserved in the original data") {
    val orderedRanks = Rank.orderedList

    Version.all foreach { version =>
      TreeType.all.foreach { treeType =>
        val tree = taxonomy.tree(version, treeType).right.value

        tree match {
          case tree: EmptyTree[TaxNode] =>
            fail(s"- Version $version, tree $treeType is empty")
          case tree: NonEmptyTree[TaxNode] =>
            val nodesWithAncestorsWithLowerRanks =
              tree.allPositions.filter { pos =>
                val node = tree(pos)
                node.rank != Rank.NoRank && {
                  closestAncestorWithRank(tree, pos) match {
                    case None => false
                    case Some(ancestor) =>
                      val ancestorRankIdx = orderedRanks.indexOf(ancestor.rank)
                      val nodeRankIdx     = orderedRanks.indexOf(node.rank)
                      ancestorRankIdx > nodeRankIdx
                  }
                }
              }

            val numBadNodes = nodesWithAncestorsWithLowerRanks.length

            assert(
              numBadNodes == 0,
              s"""- Version $version, tree $treeType contains $numBadNodes
              | nodes that break the hierarchy; i.e., the closest ancestor of
              | those nodes with a meaningful rank (different than NoRank) is
              | of a rank more specific than the node's rank (e.g., the node's
              | Rank is Species and the ancestor's rank is
              | Forma).""".stripMargin.replaceAll("/n", "")
            )
        }
      }
    }
  }

}
