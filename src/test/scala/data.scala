package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy._
import helpers._

object data {

  private val readTree = (readTaxTreeFromFiles _).tupled

  private def downloadTree(version: Version,
                           treeType: TreeType): Error + TaxTree =
    downloadTreeIfNotInLocalFolder(version, treeType).flatMap { readTree(_) }

  private def getTrees(version: Version): Error + Set[TaxTree] =
    for {
      _             <- createDirectories(version)
      good          <- downloadTree(version, TreeType.Good)
      environmental <- downloadTree(version, TreeType.Environmental)
      unclassified  <- downloadTree(version, TreeType.Unclassified)
    } yield {
      Set(good, environmental, unclassified)
    }

  lazy val treesv0_4_0 = getTrees(Version.v0_4_0)

  def trees(version: Version): Error + Set[TaxTree] =
    version match {
      case Version.v0_4_0 => treesv0_4_0
    }
}
