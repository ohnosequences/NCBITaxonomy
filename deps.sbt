libraryDependencies ++= Seq(
  "ohnosequences" %% "api-ncbitaxonomy" % "0.1.0-22-g162e052",
  "ohnosequences" %% "trees"            % "0.0.0-12-g083a074-SNAPSHOT"
) ++ testDependencies

val testDependencies = Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.0.1",
  "org.scalatest" %% "scalatest"       % "3.0.5" % Test
)
