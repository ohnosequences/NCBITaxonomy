package ohnosequences.db

import ohnosequences.s3._
import ohnosequences.db.ncbitaxonomy.{
  treeData,
  treeDataFile,
  treeShape,
  treeShapeFile
}

package object taxonomy {
  type +[A, B] = Either[A, B]

  type ScientificName = db.ncbitaxonomy.ScientificName

  def s3Prefix(version: Version): S3Folder =
    s3"resources.ohnosequences.com" /
      "db" /
      "taxonomy" /
      version.name /

  val environmentalS3(version): S3Folder =
    s3Prefix(version) / "environmental"

  val unclassifiedS3(version): S3Folder =
    s3Prefix(version) / "unclassified"

  val goodS3(version): S3Folder =
    s3Prefix(version) / "good"

  val fullTree(version: Version): TreeInS3 =
    TreeInS3(
      treeData(version.ncbiVersion),
      treeShape(version.ncbiVersion)
    )

  val environmentalTree(version: Version): TreeInS3 =
    TreeInS3(
      environmentalS3(version) / treeDataFile,
      environmentalS3(version) / treeShapeFile
    )

  val unclassifiedTree: TreeInS3 =
    TreeInS3(
      unclassifiedS3(version) / treeDataFile,
      unclassifiedS3(version) / treeShapeFile
    )

  val goodTree: TreeInS3 =
    TreeInS3(
      goodS3(version) / treeDataFile,
      goodS3(version) / treeShapeFile
    )

}
