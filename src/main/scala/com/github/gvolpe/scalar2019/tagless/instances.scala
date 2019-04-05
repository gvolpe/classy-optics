package com.github.gvolpe.scalar2019.tagless

import cats._
import cats.mtl._

object instances {
  object mtl extends DeriveMtlInstances
}

private[tagless] abstract class DeriveMtlInstances {

  implicit def deriveApplicativeAsk[F[_], G[_]: Applicative, A](
      implicit f: F ~> G,
      ev: ApplicativeAsk[F, A]
  ): ApplicativeAsk[G, A] =
    new DefaultApplicativeAsk[G, A] {
      val applicative: Applicative[G] = implicitly
      def ask: G[A]                   = f(ev.ask)
    }

}
