
```scala
package ohnosequences.ncbitaxonomy

import ohnosequences.statika._
import ohnosequences.awstools.s3._
import ohnosequencesBundles.statika._
import com.thinkaurelius.titan.core.TitanFactory
import com.bio4j.titan.util.DefaultTitanGraph
import org.apache.commons.configuration.Configuration
import com.bio4j.titan.model.ncbiTaxonomy.TitanNCBITaxonomyGraph

case object ncbiTaxonomyBundle extends AnyBio4jDist {

  lazy val s3folder: S3Folder = S3Folder("resources.ohnosequences.com", "16s/bio4j-taxonomy/")

  lazy val configuration: Configuration = DefaultBio4jTitanConfig(dbLocation)

  // the graph; its only (direct) use is for indexes
  // FIXME: this works but still with errors, should be fixed (something about transactions)
  lazy val graph: TitanNCBITaxonomyGraph =
    new TitanNCBITaxonomyGraph(
      new DefaultTitanGraph(TitanFactory.open(configuration))
    )
}

```




[main/scala/api.scala]: api.scala.md
[main/scala/bundle.scala]: bundle.scala.md
[main/scala/package.scala]: package.scala.md
[main/scala/titan.scala]: titan.scala.md
[test/scala/dummyTree.scala]: ../../test/scala/dummyTree.scala.md
[test/scala/Ncbitaxonomy.scala]: ../../test/scala/Ncbitaxonomy.scala.md
[test/scala/structuralTests.scala]: ../../test/scala/structuralTests.scala.md