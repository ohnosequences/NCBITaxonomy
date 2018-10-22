libraryDependencies ++= Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.1.0-67-gf3ce6dd-SNAPSHOT"
) ++ testDependencies

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
