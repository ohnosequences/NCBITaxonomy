package ohnosequences.ncbitaxonomy

import com.bio4j.titan.model.ncbiTaxonomy.TitanNCBITaxonomyGraph

case object api {

  trait TaxonomyGraph[V] extends Any {

    def getNode(id: String): Option[V]
    def root: V
  }

  trait Taxon[V] extends Any {

    def v: V

    def       id: String
    def     name: String
    def rankName: String
    /* Note that this is an option because the root has no parent */
    def   parent: Option[V]
    def rankNumber: Int

    final def rank: String = s"${this.rankNumber}: ${this.rankName}"

    def ancestors: Seq[V]
  }

  implicit final class ncbiTitanGraph(val graph: TitanNCBITaxonomyGraph) extends AnyVal with TaxonomyGraph[Node] {

    def getNode(id: String): Option[Node] =
      optional(graph.nCBITaxonIdIndex.getVertex(id))

    // NOTE the root will always be there. Would be nice to have this in bio4j
    def root: Node =
      getNode(1.toString).get
  }

  implicit final class ncbiTitanTaxon(val node: Node) extends AnyVal with Taxon[Node] {

    type V = Node
    def v = node

    def       id: String = node.id()
    def     name: String = Option( node.name() ).getOrElse("")
    def rankName: String = Option( node.taxonomicRank() ).getOrElse("")
    /* Note that this is an option because the root has no parent */
    def   parent: Option[Node] = optional(node.ncbiTaxonParent_inV)

    def rankNumber: Int =
      rankName.trim.toLowerCase match {
        case "superkingdom"     => 1
        case "kingdom"          => 2
        case "superphylum"      => 3
        case "phylum"           => 4
        case "subphylum"        => 5
        case "class"            => 6
        case "subclass"         => 7
        case "order"            => 8
        case "suborder"         => 9
        case "family"           => 10
        case "subfamily"        => 11
        case "tribe"            => 12
        case "subtribe"         => 13
        case "genus"            => 14
        case "subgenus"         => 15
        case "species group"    => 16
        case "species subgroup" => 17
        case "species"          => 18
        case "subspecies"       => 19
        // "no rank"
        case _                  => parent.fold(0){ _.rankNumber } + 1
      }

    def ancestors: Ancestors = {

      @annotation.tailrec
      def ancestors_rec(n: Node, acc: Ancestors): Ancestors =
        n.parent match {
          case None     => n +: acc
          case Some(p)  => ancestors_rec(p, n +: acc)
        }

      ancestors_rec(node, Seq())
    }
  }

  implicit final class Taxa[V](val nodes: Traversable[V]) extends AnyVal {

    def lowestCommonAncestor(graph: TaxonomyGraph[V])(implicit conv: V => Taxon[V]): V = {

      def longestCommonPrefix(path1: Seq[V], path2: Seq[V]): Seq[V] = {
        (path1 zip path2)
          .takeWhile { case (n1, n2) =>
            n1.id == n2.id
          }.map { _._1 }
      }

      nodes
        .map(_.ancestors)
        .reduceOption(longestCommonPrefix)
        .flatMap(_.lastOption)
        .getOrElse(graph.root)
    }
  }
}
