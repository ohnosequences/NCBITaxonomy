libraryDependencies ++= Seq(
  "ohnosequences" %% "api-ncbitaxonomy" % "0.1.0-29-g34d64a4",
  "ohnosequences" %% "trees"            % "0.0.0-33-gb545edb"
) ++ testDependencies

val testDependencies = Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.0.1",
  "org.scalatest" %% "scalatest"       % "3.0.5" % Test
)
