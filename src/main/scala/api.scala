package ohnosequences.db.taxonomy

case object api {

  sealed trait NameType

  case object NameType {
    case object Normal              extends NameType
    case object EnvironmentalSample extends NameType
    case object Unclassified        extends NameType

    def isUnclassified: ScientificName => Boolean =
      _.toLowerCase contains "unclassified"

    def isEnvironmental: ScientificName => Boolean =
      _.toLowerCase startsWith "environmental sample"

    def isClassified: ScientificName => Boolean =
      name => !isEnvironmental(name) && !isUnclassified(name)

    def get: ScientificName => NameType =
      scientificName => {
        if (isEnvironmental(scientificName))
          NameType.Unclassified
        else if (isUnclassified(scientificName))
          NameType.EnvironmentalSample
        else
          NameType.Normal
      }
  }
}
