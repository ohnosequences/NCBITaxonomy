package ohnosequences.db.taxonomy

import ohnosequences.db.ncbitaxonomy.{Version => NCBIVersion}
import ohnosequences.s3.S3Object

sealed abstract class Version {
  val name: String
  val ncbiVersion: NCBIVersion
}

/** All versions of the database */
case object Version {

  /** This points to the last version */
  val latest: Version   = _0_4_0
  val all: Set[Version] = Set(_0_4_0)

  case object _0_4_0 extends Version {
    val name        = "0.4.0"
    val ncbiVersion = NCBIVersion._0_1_0
  }
}

final case class TreeInS3(data: S3Object, shape: S3Object)
