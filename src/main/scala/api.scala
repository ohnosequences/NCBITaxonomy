package ohnosequences.db.taxonomy

case object api {

  trait TaxonomyGraph[V] extends Any {

    def getTaxon(id: String): Option[V]
    def root: V
  }

  trait Taxon[V] extends Any {

    def v: V

    def id: String
    def name: String
    def rankName: String
    /* Note that this is an option because the root has no parent */
    def parent: Option[V]
    def rankNumber(implicit conv: V => Taxon[V]): Int =
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
        case _ => parent.fold(0) { _.rankNumber } + 1
      }

    final def rank(implicit conv: V => Taxon[V]): String =
      s"${this.rankNumber}: ${this.rankName}"

    def ancestors(implicit conv: V => Taxon[V]): Seq[V] = {

      @annotation.tailrec
      def ancestors_rec(n: V, acc: Seq[V]): Seq[V] =
        n.parent match {
          case None    => n +: acc
          case Some(p) => ancestors_rec(p, n +: acc)
        }

      ancestors_rec(v, Seq())
    }
  }

  implicit final class Taxa[V](val nodes: Traversable[V]) extends AnyVal {

    def lowestCommonAncestor(graph: TaxonomyGraph[V])(
        implicit conv: V => Taxon[V]): V = {

      def longestCommonPrefix(path1: Seq[V], path2: Seq[V]): Seq[V] =
        (path1 zip path2)
          .takeWhile {
            case (n1, n2) =>
              n1.id == n2.id
          }
          .map { _._1 }

      nodes
        .map(_.ancestors)
        .reduceOption(longestCommonPrefix)
        .flatMap(_.lastOption)
        .getOrElse(graph.root)
    }
  }
}
