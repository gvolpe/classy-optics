package com.github.gvolpe.sbtb2018

import cats.Applicative
import cats.effect._
import cats.mtl._
import cats.syntax.all._
import com.olegpy.meow.hierarchy._
import config._

object demo2 extends IOApp {

  implicit val configReader: HasAppConfig[IO] =
    new ApplicativeAsk[IO, AppConfig] {
      override val applicative: Applicative[IO] = implicitly
      override def ask: IO[AppConfig]           = loadConfig[IO]
      override def reader[A](f: AppConfig => A) = ask.map(f)
    }

  override def run(args: List[String]): IO[ExitCode] =
    new Main[IO].foo.as(ExitCode.Success)

}

class Main[F[_]: HasAppConfig: Sync] {

  val p1 = new ProgramOne[F]
  val p2 = new ProgramTwo[F]
  val p3 = new ProgramThree[F]

  def foo: F[Unit] =
    for {
      _ <- p1.foo
      _ <- p2.foo
      _ <- p3.foo
    } yield ()

}
