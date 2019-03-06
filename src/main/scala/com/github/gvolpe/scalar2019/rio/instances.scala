package com.github.gvolpe.scalar2019.rio

import cats._
import cats.mtl._
import scalaz.zio._

object instances {
  object mtl extends CatzMtlInstances
}

private[rio] trait CatzMtlInstances {

  implicit def zioApplicativeAsk[R, E](implicit ev: Applicative[ZIO[R, E, ?]]): ApplicativeAsk[ZIO[R, E, ?], R] =
    new DefaultApplicativeAsk[ZIO[R, E, ?], R] {
      val applicative: Applicative[ZIO[R, E, ?]] = ev
      def ask: ZIO[R, Nothing, R]                = ZIO.environment
    }

}