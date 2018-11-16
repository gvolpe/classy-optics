package com.github.gvolpe

import cats.data.Kleisli
import cats.effect._
import cats.mtl.instances.all._
import config._

object demo1 extends IOApp {

  val p1 = new ProgramOne[Kleisli[IO, AppConfig, ?]]
  val p2 = new ProgramTwo[Kleisli[IO, HttpServerConfig, ?]]
  val p3 = new ProgramThree[Kleisli[IO, ServiceConfig, ?]]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- loadConfig[IO]
      _ <- p1.foo.run(config)
      _ <- p2.foo.run(config.httpServer)
      _ <- p3.foo.run(config.service)
    } yield ExitCode.Success

}
