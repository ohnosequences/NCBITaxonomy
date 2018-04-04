package ohnosequences.db.taxonomy

import java.io.File

package object test {
  val baseDir   = new File("./data/in")
  val namesFile = baseDir.toPath.resolve("names.dmp").toFile
  val nodesFile = baseDir.toPath.resolve("nodes.dmp").toFile
}
