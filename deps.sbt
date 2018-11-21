libraryDependencies ++= Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.2.0"
) ++ testDependencies

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
