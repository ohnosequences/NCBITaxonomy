package ohnosequences.db.taxonomy

import ohnosequences.api.ncbitaxonomy.TaxID

object data {
  val rootID: TaxID     = 1
  val bacteriaID: TaxID = 2
  val archaeaID: TaxID  = 2157

  // The taxonomy root, that has no parent and the only children it has are
  // bacteria and archaea nodes. This intends to overwrite the NCBI root, that
  // contains several nodes uninteresting for 16S. See a little bit of context
  // in https://github.com/ohnosequences/db.taxonomy/issues/12
  val root = rootID -> ((None, Array(bacteriaID, archaeaID)))
}
