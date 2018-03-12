# Taxonomy

As input, we have the *full tree*; i.e., the tree from [`db.ncbitaxonomy`](https://github.com/ohnosequences/db.ncbitaxonomy).

In addition to the *full tree*, we need to generate two more trees: the *unclassified tree* and the *environmental tree*. The method for generating these trees is:

1. Select the nodes that meet the criteria for the tree we are building (see *Unclassified Tree* section for the *unclassified tree*, and *Environmental Samples Tree* section for the *environmental tree*).
2. For each selected node in the first step, select all its descendants down to the leaves.
3. For each selected node in the first step, select all its ancestors up to the root.
4. Build the tree with all the previous selected nodes.

There is a third tree: the *good tree*, which is defined in the *Good Tree* section.

## Unclassified Tree

Nodes are deemed *unclassified* when their *scientific name* contains (ignore case) the word `unclassified`; these generate the **unclassified tree**.

## Environmental Samples Tree

Nodes are deemed *environmental* when their *scientific name* has prefix (ignore case) `environmental samples`; these generate the **environmental tree**.

## Good Tree

The *good tree* is formed by all nodes whose ancestors (including itself and up to the root) are not *unclassified* nor *environmental* nodes.

# File structure

We have the following relations:

1. `parent` (the tree structure)
2. `scientific_name`
3. `rank`

Each of the previous relations is stored as two `csv` files (both directions of the relation: in and out): first column source, second column target. For example, for the parent-child relation, we have two files:

* `children.in`: where each row has the parent as first column and all its children as second column: e.g.:
```
${rootID}, ${BacteriaID}|${ArchaeaID}|...
.
.
.
```

* `children.out`: where each row has a node in the first column and its only parent as second column: e.g.:

```
${BacteriaID}, ${rootID}
${ArchaeaID}, ${rootID}
.
.
.
```

For all these entities we use their obvious `String` representation.

Of course, we have one set of these files for each of the trees we provide (*full*, *good*, *unclassified* and *environmental* trees).

## Tree mappings

The inclusions of the other trees in the full one could be represented in the same way as with any other relation, the source and target being the same ID. Alternatively, we could represent the corresponding membership predicate.
