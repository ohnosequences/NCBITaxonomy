package ohnosequences.ncbitaxonomy

case object api {

  trait TaxonomyGraph[V] extends Any {

    def getTaxon(id: String): Option[V]
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
