package ohnosequences.db

import ohnosequences.s3._
import ohnosequences.db
import ohnosequences.db.ncbitaxonomy.{treeDataFile, treeShapeFile, io}
import ohnosequences.forests._

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode
  type TaxTree = Tree[TaxNode]

  def taxTreeFromFiles(treeFiles: TreeFiles): Error + TaxTree = {
    val readResult = readTaxTreeFromFiles(treeFiles.data, treeFiles.shape)

    readResult.fold(
      err => err.fold(Error.FileError, Error.SerializationError),
      identity
    )
  }
    
}
