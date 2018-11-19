package ohnosequences.db

import ohnosequences.db
import ohnosequences.forests.Tree

package object taxonomy {
  type +[A, B] = Either[A, B]

  type TaxNode = db.ncbitaxonomy.TaxNode
  type TaxTree = Tree[TaxNode]

  /** Returns the tree, if possible, for a version of the taxonomy and
    * a kind of tree (Good, Environmental, Unclassified)
    *
    * @param version a [[Version]] of the taxonomy
    * @param treeType a [[TreeType]]
    *
    * @return a Left(error) if some error arises downloading the tree (if
    * not already in the [[localFolder]] directory) or reading the downloaded
    * files
    */
  def tree(version: Version, treeType: TreeType): Error + TaxTree = {
    val readTree = (helpers.readTaxTreeFromFiles _).tupled

    for {
      _     <- helpers.createDirectories(version)
      files <- helpers.downloadTreeIfNotInLocalFolder(version, treeType)
      tree  <- readTree(files)
    } yield {
      tree
    }
  }
}
