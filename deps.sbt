libraryDependencies ++= Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.1.0-63-g3953074"
) ++ testDependencies

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
