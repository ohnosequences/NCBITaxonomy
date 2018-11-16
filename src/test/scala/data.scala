package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy.{Version, helpers, Error}
import helpers._
import java.io.File

object data {

  private def getTrees(version: Version): Error + Set[(File, File)] = {
    for {
      _             <- createDirectories(version)
      good          <- downloadTreeIfNotInLocalFolder(version, TreeType.Good)
      environmental <- downloadTreeIfNotInLocalFolder(version, TreeType.Environmental)
      unclassified  <- downloadTreeIfNotInLocalFolder(version, TreeType.Unclassified)
    } yield {
      Set(good, environmental, unclassified)
    }
  }

  lazy val treesv0_4_0  = getTrees(Version.v0_4_0)

  def trees(version: Version): Error + File =
    version match {
      case Version.v0_4_0  => treev0_4_0
    }
}
