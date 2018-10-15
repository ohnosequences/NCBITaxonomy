libraryDependencies ++= Seq(
  "ohnosequences" %% "api-ncbitaxonomy" % "0.1.0-31-g2e286b7",
  "ohnosequences" %% "trees"            % "0.0.0-39-gf56d086"
) ++ testDependencies

val testDependencies = Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.0.1",
  "org.scalatest" %% "scalatest"       % "3.0.5" % Test
)
