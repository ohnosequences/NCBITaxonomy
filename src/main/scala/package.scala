package ohnosequences.db

import ohnosequences.api.ncbitaxonomy.ScientificName

package object ncbitaxonomy {
  sealed trait NameType

  case object NameType {
    case object Normal              extends NameType
    case object EnvironmentalSample extends NameType
    case object Unclassified        extends NameType

    def get: ScientificName => NameType =
      scientificName => {
        val name = scientificName.name.toLowerCase

        if (name contains "unclassified")
          NameType.Unclassified
        else if (name startsWith "environmental samples")
          NameType.EnvironmentalSample
        else
          NameType.Normal
      }
  }
}
