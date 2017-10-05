name          := "ncbitaxonomy"
organization  := "ohnosequences"
description   := "A Scala wrapper for Bio4j NCBI taxonomy data and API"
bucketSuffix  := "era7.com"

crossScalaVersions := Seq("2.11.11", "2.12.3")
scalaVersion := crossScalaVersions.value.max

scalacOptions ++= Seq(
  "-Ybreak-cycles"
)

libraryDependencies ++= Seq(
  "ohnosequences-bundles" %% "bio4j-dist" % "0.4.0",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

dependencyOverrides ++= Seq(
  "com.google.guava" % "guava" % "14.0.1"
)
