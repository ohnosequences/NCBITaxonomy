libraryDependencies ++= Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.1.0-65-g6dfc06a-SNAPSHOT"
) ++ testDependencies

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
