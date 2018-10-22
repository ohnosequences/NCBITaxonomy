package ohnosequences.db.taxonomy

sealed abstract class TreeType(val name: String)

case object TreeType {

  final object Full extends TreeType("full")
  type Full = Full.type

  final object Good extends TreeType("good")
  type Good = Good.type

  final case object Environmental extends TreeType("environmental")
  type Environmental = Environmental.type

  final case object Unclassified extends TreeType("unclassified")
  type Unclassified = Unclassified.type

  val isUnclassified: TaxNode => Boolean =
    _.name.toLowerCase contains "unclassified"

  val isEnvironmental: TaxNode => Boolean =
    _.name.toLowerCase startsWith "environmental sample"

  val isGood: TaxNode => Boolean =
    node => !isEnvironmental(node) && !isUnclassified(node)

  val get: TaxNode => TreeType =
    node => {
      if (isEnvironmental(node): @inline)
        TreeType.Unclassified
      else if (isUnclassified(node): @inline)
        TreeType.Environmental
      else
        TreeType.Good
    }
}
