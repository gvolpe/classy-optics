package com.github.gvolpe.scalar2019.tagless.alt

import cats._
import cats.data.Kleisli
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

  implicit def taskGenReader[R]: BiReader[TaskR, R] =
    new BiReader[TaskR, R] {
      def runReader[A](fa: TaskR[R, A])(env: R): Task[A] = fa.provide(env)
      def unread[A](fa: Task[A]): TaskR[R, A]            = fa
    }

}

private[tagless] trait TransReaderInstances {

  implicit def kleisliGenReader[F[_], R]: TransReader[Kleisli, F, R] =
    new TransReader[Kleisli, F, R] {
      def runReader[A](fa: Kleisli[F, R, A])(env: R): F[A] = fa.run(env)
      def unread[A](fa: F[A]): Kleisli[F, R, A]            = Kleisli(_ => fa)
    }

}
