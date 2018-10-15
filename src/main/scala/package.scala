package ohnosequences.db

import ohnosequences.s3._
import ohnosequences.db
import ohnosequences.db.ncbitaxonomy.{
  treeData,
  treeDataFile,
  treeShape,
  treeShapeFile
}

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode

  def s3Prefix(version: Version): S3Folder =
    s3"resources.ohnosequences.com" /
      "db" /
      "taxonomy" /
      version.name /

  def environmentalS3(version: Version): S3Folder =
    s3Prefix(version) / "environmental" /

  def unclassifiedS3(version: Version): S3Folder =
    s3Prefix(version) / "unclassified" /

  def goodS3(version: Version): S3Folder =
    s3Prefix(version) / "good"

  def fullTree(version: Version): TreeInS3 =
    TreeInS3(
      treeData(version.ncbiVersion),
      treeShape(version.ncbiVersion)
    )

  def environmentalTree(version: Version): TreeInS3 =
    TreeInS3(
      environmentalS3(version) / treeDataFile,
      environmentalS3(version) / treeShapeFile
    )

  def unclassifiedTree(version: Version): TreeInS3 =
    TreeInS3(
      unclassifiedS3(version) / treeDataFile,
      unclassifiedS3(version) / treeShapeFile
    )

  def goodTree(version: Version): TreeInS3 =
    TreeInS3(
      goodS3(version) / treeDataFile,
      goodS3(version) / treeShapeFile
    )

}
