package ohnosequences.db.taxonomy

case object api {

  sealed trait NameType

  case object NameType {
    case object Normal              extends NameType
    case object EnvironmentalSample extends NameType
    case object Unclassified        extends NameType

    def isUnclassified: TaxNode => Boolean =
      _.name.toLowerCase contains "unclassified"

    def isEnvironmental: TaxNode => Boolean =
      _.name.toLowerCase startsWith "environmental sample"

    def isClassified: TaxNode => Boolean =
      node => !isEnvironmental(node) && !isUnclassified(node)

    def get: TaxNode => NameType =
      node => {
        if (isEnvironmental(node))
          NameType.Unclassified
        else if (isUnclassified(node))
          NameType.EnvironmentalSample
        else
          NameType.Normal
      }
  }
}
