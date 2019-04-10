package com.github.gvolpe.scalar2019.tagless.alt

import cats._
import cats.implicits._
import scalaz.zio.{ Task, TaskR }

/*
 * Typeclass that defines the relationship between an effect that requires
 * and environment `R` and an effect that doesn't require such environment.
 *
 * If we feed `R` to a given `F[R, A]` we should be able to eliminate the
 * environment and get an `F[Any, A]`.
 */
abstract class MkDep[F[- _, _], R] {
  def apply[A]: F[R, A] => R => F[Any, A]
}

object MkDep {
  implicit def taskMkDep[R]: MkDep[TaskR, R] =
    new MkDep[TaskR, R] {
      def apply[A]: TaskR[R, A] => R => Task[A] =
        reader => dep => reader.provide(dep)
    }
}

/*
 * Typeclass that defines a relationship between dependencies. It can be
 * seen as a natural transformation (~>) with different laws.
 *
 * It can normally be created by requiring a lawful `MkDep[F[R, ?], R]`
 */
abstract class Dependency[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

object Dependency {
  def make[F[- _, _], R](
      ga: F[Any, R]
  )(implicit f: Functor[F[Any, ?]], mk: MkDep[F, R]): F[Any, Dependency[F[R, ?], F[Any, ?]]] =
    ga.map { dep =>
      new Dependency[F[R, ?], F[Any, ?]] {
        def apply[A](fa: F[R, A]): F[Any, A] = mk[A](fa)(dep)
      }
    }
}
