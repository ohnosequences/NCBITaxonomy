
```scala
package ohnosequences

import com.thinkaurelius.titan.core._, schema._
import com.bio4j.model.ncbiTaxonomy._, vertices._
import com.bio4j.titan.model.ncbiTaxonomy._
import com.bio4j.titan.util.DefaultTitanGraph

package object ncbitaxonomy {

  type TitanNode =
    com.bio4j.model.ncbiTaxonomy.vertices.NCBITaxon[
      DefaultTitanGraph,
      TitanVertex, VertexLabelMaker,
      TitanEdge, EdgeLabelMaker
    ]

  final def optional[T](jopt: java.util.Optional[T]): Option[T] =
    if (jopt.isPresent) Some(jopt.get) else None
}

```




[test/scala/structuralTests.scala]: ../../test/scala/structuralTests.scala.md
[test/scala/Ncbitaxonomy.scala]: ../../test/scala/Ncbitaxonomy.scala.md
[test/scala/dummyTree.scala]: ../../test/scala/dummyTree.scala.md
[main/scala/package.scala]: package.scala.md
[main/scala/api.scala]: api.scala.md
[main/scala/titan.scala]: titan.scala.md
[main/scala/bundle.scala]: bundle.scala.md