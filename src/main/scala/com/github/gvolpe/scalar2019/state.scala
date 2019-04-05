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

object state extends IOApp {

  case class FooState(value: String) extends AnyVal

  def transformerProgram[F[_]: Console: Monad]: StateT[F, String, Unit] =
    for {
      current <- StateT.get[F, String]
      _ <- StateT.liftF(Console[F].putStrLn(current))
      _ <- StateT.set[F, String]("foo")
      updated <- StateT.get[F, String]
      _ <- StateT.liftF(Console[F].putStrLn(updated))
    } yield ()

  import com.olegpy.meow.prelude._ // Use Monad instance from MonadState

  def mtlProgram[F[_]: Console](implicit M: MonadState[F, FooState]): F[Unit] =
    for {
      current <- M.get
      _ <- Console[F].putStrLn(current)
      _ <- M.set(FooState("foo"))
      updated <- M.get
      _ <- Console[F].putStrLn(updated)
    } yield ()

  val p1 = mtlProgram[StateT[IO, FooState, ?]].run(FooState("bar"))

  val p2 = Ref.of[IO, FooState](FooState("bar")).flatMap { ref =>
    ref.runState { implicit ms =>
      mtlProgram[IO]
    }
  }

  val p3 = transformerProgram[IO].run("bar")

  def run(args: List[String]): IO[ExitCode] =
    p2.as(ExitCode.Success)

}
