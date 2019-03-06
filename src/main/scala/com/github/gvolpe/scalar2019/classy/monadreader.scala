package com.github.gvolpe.scalar2019
package classy

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import com.olegpy.meow.hierarchy._

object monadreader extends IOApp {
  import Program._

  implicit val configReader: HasAppConfig[IO] =
    new DefaultApplicativeAsk[IO, AppConfig] {
      override val applicative: Applicative[IO] = implicitly
      override def ask: IO[AppConfig]           = IO.pure(AppConfig(HttpServerConfig("host"), ServiceConfig("foo")))
    }

  val (p1, p2, p3) = instances[IO]

  //val p1 = one[IO] // it compiles fine
  //val p2 = two[IO] // Will not compile! The instances are derived only for the polymorphic version
  //val p3 = three[IO] // Will not compile either

  def run(args: List[String]): IO[ExitCode] =
    p2.as(ExitCode.Success)

}

object Program {

  def instances[F[_]: Console: HasAppConfig: Monad]: (F[Unit], F[Unit], F[Unit]) =
    (one[F], two[F], three[F])

  case class HttpServerConfig(value: String) extends AnyVal
  case class ServiceConfig(value: String) extends AnyVal
  case class AppConfig(httpServer: HttpServerConfig, service: ServiceConfig)

  type HasAppConfig[F[_]]        = ApplicativeAsk[F, AppConfig]
  type HasHttpServerConfig[F[_]] = ApplicativeAsk[F, HttpServerConfig]
  type HasServiceConfig[F[_]]    = ApplicativeAsk[F, ServiceConfig]

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

  def one[F[_]: Console: HasAppConfig: Monad]: F[Unit] =
    ask[F, AppConfig].flatMap(Console[F].putStrLn)

  def two[F[_]: Console: HasHttpServerConfig: Monad]: F[Unit] =
    ask[F, HttpServerConfig].flatMap(Console[F].putStrLn)

  def three[F[_]: Console: HasServiceConfig: Monad]: F[Unit] =
    ask[F, ServiceConfig].flatMap(Console[F].putStrLn)

}
