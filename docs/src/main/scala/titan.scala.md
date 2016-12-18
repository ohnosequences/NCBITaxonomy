
```scala
package ohnosequences.ncbitaxonomy

import api._
import com.bio4j.titan.model.ncbiTaxonomy.TitanNCBITaxonomyGraph

case object titan {

  implicit final class ncbiTitanGraph(val graph: TitanNCBITaxonomyGraph) extends AnyVal with TaxonomyGraph[TitanNode] {

    def getTaxon(id: String): Option[TitanNode] =
      optional(graph.nCBITaxonIdIndex.getVertex(id))

    // NOTE the root will always be there. Would be nice to have this in bio4j
    def root: TitanNode =
      getTaxon(1.toString).get
  }

  implicit final class ncbiTitanTaxon(val node: TitanNode) extends AnyVal with Taxon[TitanNode] {

    def v = node

    def       id: String = node.id()
    def     name: String = Option( node.name() ).getOrElse("")
    def rankName: String = Option( node.taxonomicRank() ).getOrElse("")
```

Note that this is an option because the root has no parent

```scala
    def   parent: Option[TitanNode] = optional(node.ncbiTaxonParent_inV)

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

    def ancestors: Seq[TitanNode] = {

      @annotation.tailrec
      def ancestors_rec(n: TitanNode, acc: Seq[TitanNode]): Seq[TitanNode] =
        n.parent match {
          case None     => n +: acc
          case Some(p)  => ancestors_rec(p, n +: acc)
        }

      ancestors_rec(node, Seq())
    }
  }
}

```




[main/scala/api.scala]: api.scala.md
[main/scala/bundle.scala]: bundle.scala.md
[main/scala/package.scala]: package.scala.md
[main/scala/titan.scala]: titan.scala.md
[test/scala/dummyTree.scala]: ../../test/scala/dummyTree.scala.md
[test/scala/Ncbitaxonomy.scala]: ../../test/scala/Ncbitaxonomy.scala.md
[test/scala/structuralTests.scala]: ../../test/scala/structuralTests.scala.md