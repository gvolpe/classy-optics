package com.github.gvolpe.scalar2019.tagless.alt

import cats.Functor
import cats.implicits._

/*
 * Typeclass that defines the relationship between an effect that requires
 * an environment `R` and an effect that doesn't require such environment.
 *
 * If we feed `R` to a given `F[A]` we should be able to eliminate the
 * environment and get a `G[A]`.
 */
abstract class MkDep[F[_], G[_], R] {
  def apply[A]: F[A] => R => G[A]
}

object MkDep {
  def apply[F[_], G[_], R](implicit ev: MkDep[F, G, R]): MkDep[F, G, R] = ev
}

/*
 * Specialized `MkDep` for Bifunctor-like data types such as `TaskR[R, ?]`.
 */
abstract class BiMkDep[F[_, _], R] extends MkDep[F[R, ?], F[Any, ?], R]

object BiMkDep {
  def apply[F[_, _], R](implicit ev: BiMkDep[F, R]): BiMkDep[F, R] = ev
}

/*
 * Specialized `MkDep` for Transformer-like data types such as `Kleisli[IO, R, ?]`.
 */
abstract class TransMkDep[F[_[_], _, _], G[_], R] extends MkDep[F[G, R, ?], G, R]

object TransMkDep {
  def apply[F[_[_], _, _], G[_], R](implicit ev: TransMkDep[F, G, R]): TransMkDep[F, G, R] = ev
}

/*
 * Typeclass that defines a relationship between dependencies. It can be
 * seen as a natural transformation (~>) with different laws.
 *
 * It can normally be created by requiring a lawful `MkDep[F, G, R]`
 */
abstract class Dependency[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

object Dependency {

  /*
   * Make a `Dependency[F, G]` after obtaining `R` from
   * an effectful operation on `G`.
   */
  def make[F[_], G[_]: Functor, R](
      ga: G[R]
  )(implicit mk: MkDep[F, G, R]): G[Dependency[F, G]] =
    ga.map { env =>
      new Dependency[F, G] {
        def apply[A](fa: F[A]): G[A] = mk[A](fa)(env)
      }
    }

  /*
   * Make a `Dependency[F[R, ?], F[Any, ?]]` after obtaining `R` from
   * an effectful operation on `F[Any, ?]`.
   */
  def make[F[_, _], R](
      ga: F[Any, R]
  )(implicit f: Functor[F[Any, ?]], mk: BiMkDep[F, R]): F[Any, Dependency[F[R, ?], F[Any, ?]]] =
    make[F[R, ?], F[Any, ?], R](ga)

  /*
   * Make a `Dependency[F[R, G, ?], G]` after obtaining `R` from
   * an effectful operation on `G`.
   */
  def makeReader[F[_[_], _, _], G[_]: Functor, R](
      ga: G[R]
  )(implicit mk: TransMkDep[F, G, R]): G[Dependency[F[G, R, ?], G]] =
    make[F[G, R, ?], G, R](ga)

}
