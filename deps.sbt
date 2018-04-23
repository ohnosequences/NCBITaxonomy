libraryDependencies ++= Seq(
  "ohnosequences" %% "api-ncbitaxonomy" % "0.1.0-23-g47f4309",
  "ohnosequences" %% "trees"            % "0.0.0-17-g172cd04"
) ++ testDependencies

val testDependencies = Seq(
  "ohnosequences" %% "db-ncbitaxonomy" % "0.0.1",
  "org.scalatest" %% "scalatest"       % "3.0.5" % Test
)
