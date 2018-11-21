package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy, taxonomy.{TreeType, Version}
import ohnosequences.forests._
import org.scalatest.EitherValues._

class AreTrees extends FunSuite {

  test("All versions have valid trees") {
    Version.all foreach { version =>
      TreeType.all.foreach { treeType =>
        val tree = taxonomy.tree(version, treeType).right.value

        assert(tree.isWellFormed, s"- Version $version has invalid tree(s)")
      }
    }
  }

}
