package com.github.gvolpe.scalar2019.rio

import cats._
import cats.mtl._
import scalaz.zio._

object instances {
  object mtl extends CatzMtlInstances with DeriveMtlInstances
}

private[rio] trait CatzMtlInstances {

  implicit def zioApplicativeAsk[R, E](implicit ev: Applicative[ZIO[R, E, ?]]): ApplicativeAsk[ZIO[R, E, ?], R] =
    new DefaultApplicativeAsk[ZIO[R, E, ?], R] {
      val applicative: Applicative[ZIO[R, E, ?]] = ev
      def ask: ZIO[R, Nothing, R]                = ZIO.environment
    }

}

private[rio] trait DeriveMtlInstances {

  def fk[R](r: R): TaskR[R, ?] ~> Task = λ[TaskR[R, ?] ~> Task](_.provide(r))

  implicit def deriveApplicativeAsk[F[_], G[_]: Applicative, A](
      implicit f: F ~> G,
      ev: ApplicativeAsk[F, A]
  ): ApplicativeAsk[G, A] =
    new DefaultApplicativeAsk[G, A] {
      val applicative: Applicative[G] = implicitly
      def ask: G[A]                   = f(ev.ask)
    }

}
