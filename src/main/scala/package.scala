package ohnosequences.db

import ohnosequences.awstools.s3._ // S3{Folder,Object} and s3"" string creation

package object taxonomy {
  private[taxonomy] type +[A, B] =
    Either[A, B]

  type ScientificName =
    String

  val version: String =
    "0.4.0"

  case object s3 {
    val prefix: S3Folder =
      s3"resources.ohnosequences.com" /
        "db" /
        "taxonomy" /
        version /

    val fullTree: S3Object =
      prefix / "full.tree"

    val environmentalTree: S3Object =
      prefix / "environmental.tree"

    val unclassifiedTree: S3Object =
      prefix / "unclassified.tree"

    val classifiedTree: S3Object =
      prefix / "classified.tree"
  }
}
