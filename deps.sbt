libraryDependencies ++= Seq(
  "ohnosequences-bundles" %% "bio4j-dist" % "0.4.0",
  "org.scalatest"         %% "scalatest"  % "3.0.4" % Test
)

dependencyOverrides ++= Seq(
  "com.google.guava" % "guava" % "14.0.1"
)
