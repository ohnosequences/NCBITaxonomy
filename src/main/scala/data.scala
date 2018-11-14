package ohnosequences.db.taxonomy

import ohnosequences.db.ncbitaxonomy.{Version => NCBIVersion}
import ohnosequences.files.digest.DigestFunction

sealed abstract class Version (val name: String) {
  val inputVersion: NCBIVersion

  override final def toString: String = name
}

case object Version {

  /** All versions of the database */
  val all: Set[Version] = Set(v0_4_0)

  /** This points to the last version */
  val latest: Version   = v0_4_0

  case object v0_4_0 extends Version("0.4.0") {
    val inputVersion = NCBIVersion._0_1_0
  }

  type v0_4_0 = v0_4_0.type
}

case object data {

  /** Returns the S3Folder for a version
    * 
    * @param version a [[Version]]
    */
  def prefix(version: Version): S3Folder =
    version match {
      // TODO: Remove this when we release first stable version
      case Version._0_4_0 =>
        s3"resources.ohnosequences.com" /
          "db" /
          "taxonomy" /
          "unstable" /
          version /
      case _ =>
        s3"resources.ohnosequences.com" /
          "db" /
          "taxonomy" /
          version /
    }

  /** Returns the path of a folder for a [[TreeType]]
    *
    * @param treeType the [[TreeType]]
    */
  def typeFolder(localFolder: File, treeType: TreeType): String =
    new File(localFolder, treeType.name)

  /** Returns the S3 object containing the mirrored data file for a taxonomy 
    * tree (full, good, environmental, unclassified)
    * 
    * @param version the [[Version]] of the taxonomy we want
    * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full, 
    * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
    */
  def treeData(version: Version, treeType: TreeType): S3Object = {
    val ncbiVersion = version.inputVersion

    treeType match {
      case TreeType.Full =>
        db.ncbitaxonomy.treeData(ncbiVersion)
      case _  =>
        prefix(version) / treeType / treeDataFile
    }
  }

  /** Returns the S3 object containing the mirrored shape file for a taxonomy
    * tree (full, good, environmental, unclassified)
    * 
    * @param version the [[Version]] of the taxonomy we want
    * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full, 
    * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
    */
  def treeShape(version: Version, treeType: TreeType): S3Object = {
    val ncbiVersion = version.inputVersion

    treeType match {
      case TreeType.Full =>
        db.ncbitaxonomy.treeShape(ncbiVersion)
      case _  =>
        prefix(version) / treeType / treeShapeFile
    }
  }

  /** A set with all the S3 objects generated in the input version
    * 
    * @param version the [[Version]] we want to list the objects for
    */
  def everything(version: Version): Set[S3Object] =
    TreeType.all.map { treeData(version, _) } | TreeType.all.map { shapeData(version, _) }

  val hashingFunction: DigestFunction = DigestFunction.SHA512

  // Local files
  case object local {

    /** Returns the local path of the data file for a taxonomy tree
      * (full, good, environmental, unclassified)
      * 
      * @param version the [[Version]] of the taxonomy we want
      * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full, 
      * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
      * @param localFolder where the path for the tree is going to be built
      */
    def treeData(version: Version, treeType: TreeType, localFolder: File): File = {
      new File(typeFolder(localFolder, treeType), treeDataFile)
    }

    /** Returns the local path of the shape file for a taxonomy tree
      * (full, good, environmental, unclassified)
      * 
      * @param version the [[Version]] of the taxonomy we want
      * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full, 
      * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
      * @param localFolder where the path for the tree is going to be built
      */
    def treeShape(version: Version, treeType: TreeType, localFolder: File): File = {
      new File(typeFolder(localFolder, treeType), treeShapeFile)
    }
  }
}
