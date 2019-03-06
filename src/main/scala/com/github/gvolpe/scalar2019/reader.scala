package com.github.gvolpe.scalar2019

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import cats.mtl._
import cats.mtl.instances.all._

object reader extends IOApp {

  type Context = String

  implicit val ioApplicativeAsk: ApplicativeAsk[IO, Context] =
    new DefaultApplicativeAsk[IO, Context] {
      override val applicative: Applicative[IO] = implicitly
      override def ask: IO[Context]             = IO.pure("env 3")
    }

  def transformerReader[F[_]: Console]: Kleisli[F, Context, Unit] =
    Kleisli(Console[F].putStrLn)

  type HasContext[F[_]] = ApplicativeAsk[F, Context]

  def mtlReader[F[_]: Console: HasContext: Monad]: F[Unit] =
    ApplicativeAsk[F, Context].ask.flatMap(Console[F].putStrLn)

  val p1 = transformerReader[IO].run("env 1")

  val p2 = mtlReader[Kleisli[IO, Context, ?]].run("env 2")

  val p3 = mtlReader[IO]

  def run(args: List[String]): IO[ExitCode] =
    p3.as(ExitCode.Success)

}
