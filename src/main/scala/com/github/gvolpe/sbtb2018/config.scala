package com.github.gvolpe.sbtb2018

import cats.effect._
import cats.mtl._
import cats.syntax.all._
import config._
import pureconfig.generic.auto._

object config {

  case class Host(value: String) extends AnyVal
  case class Port(value: Int) extends AnyVal
  case class HttpServerConfig(host: Host, port: Port)

  case class PublicKeyConfig(value: String) extends AnyVal
  case class ServiceConfig(publicKey: PublicKeyConfig)

  case class AppConfig(httpServer: HttpServerConfig, service: ServiceConfig)

  def loadConfig[F[_]: Sync]: F[AppConfig] =
    Sync[F].delay(pureconfig.loadConfigOrThrow[AppConfig])

  type HasAppConfig[F[_]]        = ApplicativeAsk[F, AppConfig]
  type HasHttpServerConfig[F[_]] = ApplicativeAsk[F, HttpServerConfig]
  type HasServiceConfig[F[_]]    = ApplicativeAsk[F, ServiceConfig]

}

class ProgramOne[F[_]: HasAppConfig: Sync] {

  def foo: F[Unit] =
    ApplicativeAsk[F, AppConfig].ask.flatMap { config =>
      Sync[F].delay(println(config))
    }

}

class ProgramTwo[F[_]: HasHttpServerConfig: Sync] {

  def foo: F[Unit] =
    ApplicativeAsk[F, HttpServerConfig].ask.flatMap { config =>
      Sync[F].delay(println(config))
    }

}

class ProgramThree[F[_]: HasServiceConfig: Sync] {

  def foo: F[Unit] =
    ApplicativeAsk[F, ServiceConfig].ask.flatMap { config =>
      Sync[F].delay(println(config))
    }

}
