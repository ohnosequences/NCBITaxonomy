package ohnosequences.db

package object taxonomy {
  private[taxonomy] type +[A, B] =
    Either[A, B]
}
