package com.github.gvolpe.scalar2019

import cats._
import cats.data._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import cats.mtl._
import cats.mtl.instances.all._
import com.olegpy.meow.effects._
import com.olegpy.meow.hierarchy._

object readerstate extends IOApp {

  implicit val ioApplicativeAsk: ApplicativeAsk[IO, Int] =
    new DefaultApplicativeAsk[IO, Int] {
      override val applicative: Applicative[IO] = implicitly
      override def ask: IO[Int]                 = IO.pure(666)
    }

  def transformerProgram[F[_]: Console: Monad]: ReaderT[StateT[F, String, ?], Int, Unit] =
    for {
      current <- Kleisli[StateT[F, String, ?], Int, String](_ => StateT.get[F, String])
      _ <- Kleisli[StateT[F, String, ?], Int, Unit](_ => StateT.liftF(Console[F].putStrLn(current)))
      _ <- Kleisli[StateT[F, String, ?], Int, Unit](n => StateT.set[F, String](s"foo #$n"))
      updated <- Kleisli[StateT[F, String, ?], Int, String](_ => StateT.get[F, String])
      _ <- Kleisli[StateT[F, String, ?], Int, Unit](_ => StateT.liftF(Console[F].putStrLn(updated)))
    } yield ()

  import com.olegpy.meow.prelude._ // Use Monad instance from MonadState

  def mtlProgram[F[_]: Console: ApplicativeAsk[?[_], Int]](implicit M: MonadState[F, String]): F[Unit] =
    for {
      current <- M.get
      _ <- Console[F].putStrLn(current)
      n <- ApplicativeAsk[F, Int].ask
      _ <- M.set(s"foo #$n")
      updated <- M.get
      _ <- Console[F].putStrLn(updated)
    } yield ()

  val p1 = transformerProgram[IO].run(87).run("foo")

  val p2 = mtlProgram[ReaderT[StateT[IO, String, ?], Int, ?]].run(123).run("bar")

  val p3 = Ref.of[IO, String]("wow").flatMap { ref =>
    ref.runState { implicit ms =>
      mtlProgram[IO]
    }
  }

  def run(args: List[String]): IO[ExitCode] =
    p3.as(ExitCode.Success)

}
