package ohnosequences.db.taxonomy.test

import org.scalatest.FunSuite
import ohnosequences.db.taxonomy, taxonomy.{Version, helpers}

class Existence extends FunSuite {

  test("All supported versions exist") {
    Version.all foreach { version =>
      val objs = taxonomy.data.everything(version)

      objs.foreach { obj =>
        assert(
          helpers.objectExists(obj),
          s"- Version $version is not complete: object $obj does not exist.")
      }
    }
  }
}
