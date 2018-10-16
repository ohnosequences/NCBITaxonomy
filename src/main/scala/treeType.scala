package ohnosequences.db.taxonomy

sealed abstract class TreeType(name: String)

case object TreeType {
  final case class Full          extends TreeType("full")
  final case class Good          extends TreeType("good")
  final case class Environmental extends TreeType("environmental")
  final case class Unclassified  extends TreeType("unclassified")

  val isUnclassified: TaxNode => Boolean =
    _.name.toLowerCase contains "unclassified"

  val isEnvironmental: TaxNode => Boolean =
    _.name.toLowerCase startsWith "environmental sample"

  val isGood: TaxNode => Boolean =
    node => !isEnvironmental(node) && !isUnclassified(node)

  val get: TaxNode => TreeType =
    node => {
      if (isEnvironmental(node))
        TreeType.Unclassified
      else if (isUnclassified(node))
        TreeType.Environmental
      else
        TreeType.Good
    }
}

