package com.github.gvolpe.scalar2019.tagless.alt

import cats.Functor
import cats.implicits._

/*
 * Generalized Reader for any `F[_]` that can eliminate the environment `R`
 * and in effect produce a `G[_]`.
 *
 * It abstracts over `provide` and `run`, for `ZIO` and `Kleisli`, respectively.
 *
 * Examples:
 *
 * - `Kleisli[IO, R, A] => R => IO[A]`
 * - `TaskR[R, A] => R => Task[A]`
 */
abstract class GenReader[F[_], G[_], R] {
  def apply[A]: F[A] => R => G[A]
}

object GenReader {
  def apply[F[_], G[_], R](implicit ev: GenReader[F, G, R]): GenReader[F, G, R] = ev
}

/*
 * Specialized `GenReader` for Bifunctor-like data types such as `TaskR[R, ?]`.
 */
abstract class BiReader[F[_, _], R] extends GenReader[F[R, ?], F[Any, ?], R]

object BiReader {
  def apply[F[_, _], R](implicit ev: BiReader[F, R]): BiReader[F, R] = ev
}

/*
 * Specialized `GenReader` for Transformer-like data types such as `Kleisli[IO, R, ?]`.
 */
abstract class TransReader[F[_[_], _, _], G[_], R] extends GenReader[F[G, R, ?], G, R]

object TransReader {
  def apply[F[_[_], _, _], G[_], R](implicit ev: TransReader[F, G, R]): TransReader[F, G, R] = ev
}

/*
 * Typeclass that defines a relationship between dependencies. It can be
 * seen as a natural transformation (~>) with different laws.
 *
 * It can normally be created by requiring a lawful `GenReader[F, G, R]`
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
  )(implicit reader: GenReader[F, G, R]): G[Dependency[F, G]] =
    ga.map { env =>
      new Dependency[F, G] {
        def apply[A](fa: F[A]): G[A] = reader[A](fa)(env)
      }
    }

  /*
   * Make a `Dependency[F[R, ?], F[Any, ?]]` after obtaining `R` from
   * an effectful operation on `F[Any, ?]`.
   */
  def make[F[_, _], R](
      ga: F[Any, R]
  )(implicit f: Functor[F[Any, ?]], reader: BiReader[F, R]): F[Any, Dependency[F[R, ?], F[Any, ?]]] =
    make[F[R, ?], F[Any, ?], R](ga)

  /*
   * Make a `Dependency[F[R, G, ?], G]` after obtaining `R` from
   * an effectful operation on `G`.
   */
  def makeReader[F[_[_], _, _], G[_]: Functor, R](
      ga: G[R]
  )(implicit reader: TransReader[F, G, R]): G[Dependency[F[G, R, ?], G]] =
    make[F[G, R, ?], G, R](ga)

}
