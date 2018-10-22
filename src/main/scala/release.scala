package ohnosequences.db.taxonomy

import ohnosequences.forests.{EmptyTree, NonEmptyTree, Tree}, Tree._

case object release {

  def generateTree(fullTree: TaxTree, treeType: TreeType): TaxTree =
    fullTree match {
      case fullTree: EmptyTree[TaxNode] => fullTree
      case fullTree: NonEmptyTree[TaxNode] =>
        treeType match {
          case TreeType.Full => fullTree
          case TreeType.Unclassified =>
            val positions = whichPositions(fullTree) { pos =>
              TreeType.get(fullTree(pos)) == TreeType.Unclassified
            }.toSet

            coveringTree(fullTree, positions)
          case TreeType.Environmental =>
            val positions = whichPositions(fullTree) { pos =>
              TreeType.get(fullTree(pos)) == TreeType.Environmental
            }.toSet

            coveringTree(fullTree, positions)
          case TreeType.Good =>
            filter(fullTree) { node =>
              TreeType.get(node) == TreeType.Good
            }
        }
    }
}
