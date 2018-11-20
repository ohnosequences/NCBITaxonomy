libraryDependencies ++= Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.1.0-75-g4316844-SNAPSHOT"
) ++ testDependencies

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
