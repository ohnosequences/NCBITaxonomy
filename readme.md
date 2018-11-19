# db.taxonomy

[![](http://github-release-version.herokuapp.com/github/ohnosequences/db.taxonomy/release.svg)](https://github.com/ohnosequences/db.fragments16s/releases/latest)
[![](https://img.shields.io/badge/license-AGPLv3-blue.svg)](https://tldrlegal.com/license/gnu-affero-general-public-license-v3-%28agpl-3.0%29)
[![](https://img.shields.io/badge/license-ODbL-brightgreen.svg)](https://opendatacommons.org/licenses/odbl/)

Taxonomic trees derived from the NCBI one.

# Installation

Just add

```scala
resolvers += "Era7 maven releases" at "https://s3-eu-west-1.amazonaws.com/releases.era7.com"
libraryDependencies += "ohnosequences" %% "db.taxonomy" % "x.y.z"
```

to your `sbt` dependencies, where `x.y.z` is the version of the [latest release][latest-release].

# Data

Let's give a couple of preliminary definitions:
    
A *taxonomic node* is said to be:
  - `unclassified` if its scientific name contains the word `unclassified`.
  - `environmental` if its scientific name has prefix `environmental samples`.
  - `good` if its neither `unclassified` nor `environmental`.

The *covering tree* of a set of nodes in a tree is defined as the node which includes all the ancestors and all the descendants for the nodes in that set.

Having said that, we take the full taxonomic tree made available through [db.ncbitaxonomy][db.ncbitaxonomy] and we generate three other taxonomic trees from there:
  - A `good` taxonomic tree: given by nodes whose ancestors (including itself and up to the root) are neither unclassified nor environmental.
  - An `environmental` taxonomic tree: the covering tree of all the `environmental` nodes.
  - An `unclassified` taxonomic tree: the covering tree of all the `unclassified` nodes.
  

# How to access the data

## Versions

All the data in `db.taxonomy` is versioned, based on a version of our NCBI taxonomy package [db.ncbitaxonomy][db.ncbitaxonomy]. A version includes `inputVersion`, which points to a NCBI taxonomy version, and consists of a bunch of serialized trees in S3 with a `data.tree` file and a `shape.tree` file. Those files exist in `s3://resources.ohnosequences.com/db/taxonomy/` for each of the aforementioned trees, except for the full taxonomy, which is a pointer to the files in `db.ncbitaxonomy`:
  - full tree: `s3://resources.ohnosequences.com/db/ncbitaxonomy/<inputVersion>`
  - good tree: `s3://resources.ohnosequences.com/db/taxonomy/unstable/<version>/good`
  - environmental tree: `s3://resources.ohnosequences.com/db/taxonomy/unstable/<version>/environmental`
  - unclassified tree: `s3://resources.ohnosequences.com/db/taxonomy/unstable/<version>/unclassified`
  
Each of these versions is encoded as an object that extends the sealed class `Version` in [`data.scala`](src/main/scala/data.scala).

The `Set` `Version.all` contains all the releases supported. `Version.latest` is a pointer to the latest released version.

## Files

The module [`db.taxonomy.data`](src/main/scala/data.scala) contains the paths of the S3 object corresponding to the tree data and shape files for a `Version` and a `TreeType`. They be accessed evaluating the following methods over a `Version` object:

```scala
treeData : (Version, TreeType) => S3Object
treeShape: (Version, TreeType) => S3Object
```

## Tree

The taxonomic tree of a `TreeType` for a `Version`, in case it can be retrieved / read from local files, is made available through `db.taxonomy.tree`.

Folder for downloaded data is given by `data.localFolder`.

## Example

```scala
import ohnosequences.db.taxonomy
import taxonomy.{Version, TreeType}

val maybeTree = taxonomy.tree(Version.latest, TreeType.Good)

maybeTree.map { tree =>
    // do something with tree
    
}
```

## License

- The *code* which generates the database is licensed under the **[AGPLv3]** license
- The *database* itself is made available under the **[ODbLv1]** license.
- The database *contents* are available under their respective licenses. As far as we can tell all data included in *db.fragments16s* could be considered **free** for any use; do note that sequences and annotations coming from SILVA, which has a restrictive license, are excluded from *db.fragments16s*.

See the [open data commons FAQ](http://opendatacommons.org/faq/licenses/#db-versus-contents) for more on this distinction between database and contents.

[latest-release]: https://github.com/ohnosequences/db.taxonomy/releases/latest
[db.ncbitaxonomy]: https://github.com/ohnosequences/db.ncbitaxonomy
