package ohnosequences.db.taxonomy

import java.io.File

package object test {
  val baseDir = new File("./data/in")

  val namesFile = baseDir.toPath.resolve("names.dmp").toFile
  val nodesFile = baseDir.toPath.resolve("nodes.dmp").toFile

  val envFile  = baseDir.toPath.resolve("./env.csv").toFile
  val uncFile  = baseDir.toPath.resolve("./unc.csv").toFile
  val claFile  = baseDir.toPath.resolve("./cla.csv").toFile
  val fullFile = baseDir.toPath.resolve("./full.csv").toFile
}
