package ohnosequences.db

package object taxonomy {
  private[taxonomy] type +[A, B] =
    Either[A, B]

  type ScientificName =
    String

  val version: String =
    "0.2.0"

  val s3Prefix: S3Folder =
    s3"resources.ohnosequences.com" /
      "db" /
      "taxonomy" /
      version /

  val fullTree: S3Object =
    s3Prefix / "full.tree"

  val environmentalTree: S3Object =
    s3Prefix / "environmental.tree"

  val unclassifiedTree: S3Object =
    s3Prefix / "unclassified.tree"

  val classifiedTree: S3Object =
    s3Prefix / "classified.tree"
}
