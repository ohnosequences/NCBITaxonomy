package ohnosequences.db

import ohnosequences.db
import ohnosequences.forests.Tree
import java.io.File
import db.ncbitaxonomy.io

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode
  type TaxTree = Tree[TaxNode]

  def readTaxTreeFromFiles(data: File, shape: File): Error + TaxTree =
    io.readTaxTreeFromFiles(data, shape)
      .left
      .map { err =>
        err.fold(Error.FileError, Error.SerializationError)
      }

  def dumpTaxTreeToFiles(tree: TaxTree,
                         data: File,
                         shape: File): Error + (File, File) =
    io.dumpTaxTreeToFiles(tree, data, shape)
      .left
      .map(Error.FileError)

}
