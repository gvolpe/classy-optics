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

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

  def putStrLn[F[_]: Sync, A](a: A): F[Unit] = Sync[F].delay(println(a))

  def loadConfig[F[_]: Sync]: F[AppConfig] =
    Sync[F].delay(pureconfig.loadConfigOrThrow[AppConfig])

  type HasAppConfig[F[_]]        = ApplicativeAsk[F, AppConfig]
  type HasHttpServerConfig[F[_]] = ApplicativeAsk[F, HttpServerConfig]
  type HasServiceConfig[F[_]]    = ApplicativeAsk[F, ServiceConfig]

}

class ProgramOne[F[_]: HasAppConfig: Sync] {

  def foo: F[Unit] =
    ask[F, AppConfig].flatMap(putStrLn(_))

}

class ProgramTwo[F[_]: HasHttpServerConfig: Sync] {

  def foo: F[Unit] =
    ask[F, HttpServerConfig].flatMap(putStrLn(_))

}

class ProgramThree[F[_]: HasServiceConfig: Sync] {

  def foo: F[Unit] =
    ask[F, ServiceConfig].flatMap(putStrLn(_))

}
