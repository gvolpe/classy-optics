package com.github.gvolpe.scalar2019.tagless.alt

import cats._
import cats.mtl._

object instances {
  object mtl extends AltDeriveMtlInstances
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
