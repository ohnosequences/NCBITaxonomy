package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy.Version
import ohnosequences.forests._
import org.scalatest.EitherValues._

class AreTrees extends FunSuite {

  test("All versions have valid trees") {
    Version.all foreach { version =>
      val trees = data.trees(version)

      trees.right.value.foreach { tree =>
        assert(tree.isWellFormed, s"- Version $version has invalid tree(s)")
      }
    }
  }

}
