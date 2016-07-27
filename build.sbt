Nice.scalaProject

name          := "ncbitaxonomy"
organization  := "ohnosequences"
description   := "A Scala wrapper for Bio4j NCBI taxonomy data and API"

bucketSuffix  := "era7.com"

libraryDependencies ++=  Seq(
  "ohnosequences-bundles" %% "bio4j-dist" % "0.2.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % Test
)

scalacOptions ++= Seq("-Ybreak-cycles")

/* because of Option#get */
wartremoverExcluded ++= Seq(
  baseDirectory.value/"src"/"main"/"scala"/"ncbitaxonomy.scala"
)
