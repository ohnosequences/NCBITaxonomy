package ohnosequences.db

import ohnosequences.s3._
import ohnosequences.db
import ohnosequences.db.ncbitaxonomy.{treeDataFile, treeShapeFile}

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode

  def s3Prefix(version: Version): S3Folder =
    s3"resources.ohnosequences.com" /
      "db" /
      "taxonomy" /
      version.name /


  def treeData(version: Version, treeType: TreeType): S3Object =
    treeType match {
      case TreeType.Full => db.ncbiTaxonomy.treeData(version.ncbiVersion)
      case _             => s3Prefix(version) / treeType.name / treeDataFile
    }

  def treeShape(version: Version, treeType: TreeType): S3Object =
    treeType match {
      case TreeType.Full => db.ncbiTaxonomy.treeShape(version.ncbiVersion)
      case _             => s3Prefix(version) / treeType.name / treeShapeFile
    }
}
