package ohnosequences

import com.thinkaurelius.titan.core._, schema._
import com.bio4j.titan.util.DefaultTitanGraph

package object ncbitaxonomy {

  type TitanNode =
    com.bio4j.model.ncbiTaxonomy.vertices.NCBITaxon[
      DefaultTitanGraph,
      TitanVertex,
      VertexLabelMaker,
      TitanEdge,
      EdgeLabelMaker
    ]

  final def optional[T](jopt: java.util.Optional[T]): Option[T] =
    if (jopt.isPresent) Some(jopt.get) else None
}
