package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy, taxonomy.{TaxNode, TreeType, Version}
import ohnosequences.db.ncbitaxonomy.Rank
import ohnosequences.forests._
import org.scalatest.EitherValues._

class RankHierarchy extends FunSuite {

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

    recUntilRoot(initialPos)
  }

  test("All versions have valid trees") {
    val orderedRanks = List[Rank](
      Rank.Superkingdom,
      Rank.Kingdom,
      Rank.Subkingdom,
      Rank.Superphylum,
      Rank.Phylum,
      Rank.Subphylum,
      Rank.Superclass,
      Rank.Class,
      Rank.Subclass,
      Rank.Infraclass,
      Rank.Cohort,
      Rank.Superorder,
      Rank.Order,
      Rank.Suborder,
      Rank.Infraorder,
      Rank.Parvorder,
      Rank.Superfamily,
      Rank.Family,
      Rank.Subfamily,
      Rank.Tribe,
      Rank.Subtribe,
      Rank.Genus,
      Rank.Subgenus,
      Rank.SpeciesGroup,
      Rank.SpeciesSubgroup,
      Rank.Species,
      Rank.Subspecies,
      Rank.Varietas,
      Rank.Forma
    )

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
              s"""- Version $version, tree $treeType contains
              | $numBadNodes nodes that break the hierarchy; i.e., the closest
              | ancestor of those nodes with a meaningful rank (different than
              |  NoRank) is of a rank more specific than the node's rank (e.g.,
              | the node's Rank is Species and the ancestor's rank is
              | Forma).""".stripMargin.replaceAll("/n", "")
            )
        }
      }
    }
  }

}
