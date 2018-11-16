package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy.{Version, helpers}
import org.scalatest.EitherValues._
import data._

class AreTrees extends FunSuite {

  test("All versions have valid trees") {
    Version.all foreach { version =>
      val trees = data.getTrees(version)

      trees.foreach { tree =>
        assert { tree.isWellFormed,
          s"- Version $v has invalid tree(s)"
        }
      }
    }
  }
}
