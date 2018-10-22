package ohnosequences.db.taxonomy.test

import ohnosequences.db.taxonomy._

object data {

  def dataDirectory(version: Version, treeType: TreeType): File =
    new File(s"./data/in/${version.name}/${treeType.name}")

  def treeDataLocalFile(version: Version, treeType: TreeType): File =
    dataDirectory(version, treeType).toPath.resolve(treeDataFile).toFile

  def treeShapeLocalFile(version: Version, treeType: TreeType): File =
    dataDirectory(version, treeType).toPath.resolve(treeShapeFile).toFile

}
