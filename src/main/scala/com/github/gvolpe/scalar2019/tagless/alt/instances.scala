package com.github.gvolpe.scalar2019.tagless.alt

import cats._
import cats.data.Kleisli
import cats.effect.{ IO => CatsIO }
import cats.mtl._
import scalaz.zio.{ Task, TaskR }

object instances {
  object mtl extends AltDeriveMtlInstances
  object deps extends DependencyInstances
}

private[tagless] abstract class AltDeriveMtlInstances {

  implicit def deriveApplicativeAsk[F[_], G[_]: Applicative, A](
      implicit dep: Dependency[F, G],
      ev: ApplicativeAsk[F, A]
  ): ApplicativeAsk[G, A] =
    new DefaultApplicativeAsk[G, A] {
      val applicative: Applicative[G] = implicitly
      def ask: G[A]                   = dep(ev.ask)
    }

}

private[tagless] abstract class DependencyInstances extends BiReaderInstances with TransReaderInstances

private[tagless] trait BiReaderInstances {

  implicit def taskBiMkDep[R]: BiReader[TaskR, R] =
    new BiReader[TaskR, R] {
      def apply[A]: TaskR[R, A] => R => Task[A] =
        reader => env => reader.provide(env)
    }

}

private[tagless] trait TransReaderInstances {

  implicit def kleisliIOTransMkDep[R]: TransReader[Kleisli, CatsIO, R] =
    new TransReader[Kleisli, CatsIO, R] {
      def apply[A]: Kleisli[CatsIO, R, A] => R => CatsIO[A] =
        reader => env => reader.run(env)
    }

}
