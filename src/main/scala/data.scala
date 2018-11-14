package ohnosequences.db.taxonomy

import ohnosequences.db.ncbitaxonomy.{Version => NCBIVersion}
import ohnosequences.files.digest.DigestFunction

sealed abstract class Version {
  val name: String
  val ncbiVersion: NCBIVersion
}

case object Version {

  /** All versions of the database */
  lazy val all: Set[Version] = Set(_0_4_0)

  /** This points to the last version */
  val latest: Version   = _0_4_0

  case object _0_4_0 extends Version {
    val name        = "0.4.0"
    val ncbiVersion = NCBIVersion._0_1_0
  }
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
          version.name /
      case _ =>
        s3"resources.ohnosequences.com" /
          "db" /
          "taxonomy" /
          version.name /
    }

  /** Returns the path of a data file for a type of tree
    *
    * @param treeType the [[TreeType]]
    */
  def dataFile(treeType: TreeType): String =
    treeType.name + "/" + treeDataFile

  /** Returns the path of a shape file for a type of tree
    *
    * @param treeType the [[TreeType]]
    */
  def shapeFile(treeType: TreeType): String =
    treeType.name + "/" + treeShapeFile

  /** Returns the S3 object containing the mirrored data file for a taxonomy 
    * tree (full, good, environmental, unclassified)
    * 
    * @param version the [[Version]] of the taxonomy we want
    * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full, 
    * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
    */
  def treeData(version: Version, treeType: TreeType): S3Object = {
    val ncbiVersion = version.ncbiVersion

    treeType match {
      case TreeType.Full =>
        db.ncbitaxonomy.treeData(ncbiVersion)
      case _  =>
        prefix(version) / dataFile(treeType)        
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
    val ncbiVersion = version.ncbiVersion

    treeType match {
      case TreeType.Full =>
        db.ncbitaxonomy.treeShape(ncbiVersion)
      case _  =>
        prefix(version) / shapeFile(treeType)
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
      new File(localFolder, dataFile(treeType))
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
      new File(localFolder, shapeFile(treeType))
    }
  }
}
