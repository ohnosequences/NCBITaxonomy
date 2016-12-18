name          := "ncbitaxonomy"
organization  := "ohnosequences"
description   := "A Scala wrapper for Bio4j NCBI taxonomy data and API"

bucketSuffix  := "era7.com"

libraryDependencies ++= Seq(
  "ohnosequences-bundles" %% "bio4j-dist" % "0.3.0"
)

scalacOptions ++= Seq("-Ybreak-cycles")

/* because of Option#get */
wartremoverExcluded ++= Seq(
  baseDirectory.value/"src"/"main"/"scala"/"titan.scala"
)
wartremoverErrors in (Test,    compile) := Seq()
