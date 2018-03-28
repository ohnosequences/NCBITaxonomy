package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy._, api._

case object dummy {

  implicit final class NodeOps(val v: Node) extends Taxon[Node] {

    def id       = v.id
    def name     = v.name
    def rankName = v.rankName

    def parent = v.parent
  }

  sealed trait Node {

    def v        = this
    def id       = this.toString
    def name     = id
    def rankName = ""
    def parent: Option[Node]
  }

  class child(val p: Node) extends Node {

    def parent = Some(p)
  }

  case object root extends Node {

    def parent = None
  }

  // // common part
  lazy val c1 = new child(root)
  lazy val c2 = new child(c1)
  // left branch
  lazy val l1 = new child(c2)
  lazy val l2 = new child(l1)
  // right branch
  lazy val r1 = new child(c2)
  lazy val r2 = new child(r1)
  lazy val r3 = new child(r2)

  val common = Seq(root, c1, c2)

  val allNodes: Set[Node] = Set(root, c1, c2, l1, l2, r1, r2, r3)

  val id2node: Map[String, Node] = allNodes.map { n =>
    (n.id -> n)
  }.toMap

  case object dummyGraph extends TaxonomyGraph[Node] {

    def getTaxon(id: String): Option[Node] = id2node.get(id)
    def root: Node                         = dummy.root
  }

}
