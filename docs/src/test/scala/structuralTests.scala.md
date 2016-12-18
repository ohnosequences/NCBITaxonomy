
```scala
package ohnosequences.ncbitaxonomy.test

import ohnosequences.ncbitaxonomy._, api._
import dummy._

class LCATest extends org.scalatest.FunSuite {

  test("ancestors") {

    assert{ l2.ancestors == Seq(root, c1, c2, l1, l2) }
    assert{ c1.ancestors == Seq(root, c1) }
    assert{ root.ancestors == Seq(root) }
  }

  test("lowest common ancestor") {

    // Just a shortcut:
    def lca(nodes: Seq[Node]): Node = nodes lowestCommonAncestor dummyGraph

    assertResult( root )  { lca(Seq())          }
    assertResult( r3 )    { lca(Seq(r3))        }
    assertResult( root )  { lca(Seq(root, l1))  }
    assertResult( c1 )    { lca(Seq(c1,c2))     }
    assertResult( c2 )    { lca(Seq(l1, r3))    }
    assertResult( c1 )    { lca(Seq(l1, c1))    }
    assertResult( l1 )    { lca(Seq(l1, l2))    }
    assertResult( c2 )    { lca(Seq(c2, r2))    }
    assertResult( c2 )    { lca(Seq(l1, r1))    }
    assertResult( c2 )    { lca(Seq(c2, c2))    }
    // Multiple nodes:
    // left, common, right
    assertResult( c2 ) { lca(Seq(l1, c2, r2))     }
    // all on the same line
    assertResult( c1 ) { lca(Seq(c1, l2, l1, c2)) }
    // some from one branch and some from another
    assertResult( c1 ) { lca(Seq(c1, l2, r3, c2, r1)) }
  }
}

```




[main/scala/api.scala]: ../../main/scala/api.scala.md
[main/scala/bundle.scala]: ../../main/scala/bundle.scala.md
[main/scala/package.scala]: ../../main/scala/package.scala.md
[main/scala/titan.scala]: ../../main/scala/titan.scala.md
[test/scala/dummyTree.scala]: dummyTree.scala.md
[test/scala/Ncbitaxonomy.scala]: Ncbitaxonomy.scala.md
[test/scala/structuralTests.scala]: structuralTests.scala.md