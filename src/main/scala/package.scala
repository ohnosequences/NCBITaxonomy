package ohnosequences.db

import ohnosequences.s3._
import ohnosequences.db
import ohnosequences.forests.Tree
import java.io.File
import db.ncbitaxonomy.io

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode
  type TaxTree = Tree[TaxNode]

  def taxTreeFromFiles(data: File, shape: File): Error + TaxTree =
    io.readTaxTreeFromFiles(data, shape)
      .left
      .map { err =>
        err.fold(Error.FileError, Error.SerializationError)
      }

}
