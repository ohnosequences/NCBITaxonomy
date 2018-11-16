package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy, taxonomy.{Version, helpers}
import org.scalatest.EitherValues._
import data._

class Existence extends FunSuite {

  test("All supported versions exist") {
    Version.all foreach { version =>
      val objs = taxonomy.everything(version)

      objs.foreach { obj =>
        assert { helpers.objectExists(obj).right.value,
          s"- Version $v is not complete: object $obj does not exist."
        }
      }
    }
  }
}
