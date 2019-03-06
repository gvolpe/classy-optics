package com.github.gvolpe.scalar2019

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import com.olegpy.meow.hierarchy._

object module {
  type HasServiceOne[F[_]] = ApplicativeAsk[F, ServiceOne[F]]
  type HasServiceTwo[F[_]] = ApplicativeAsk[F, ServiceTwo[F]]
  type HasModule[F[_]]     = ApplicativeAsk[F, ServiceModule[F]]

  def ask[F[_], T](implicit ev: ApplicativeAsk[F, T]) = ev.ask
}

import module._

object tagless extends IOApp {

  def moduleReader(module: ServiceModule[IO]): HasModule[IO] =
    new DefaultApplicativeAsk[IO, ServiceModule[IO]] {
      override val applicative: Applicative[IO] = implicitly
      override def ask: IO[ServiceModule[IO]]   = IO.pure(module)
    }

  val one: ServiceOne[IO] = new ServiceOne[IO] {
    def get: IO[String] = IO.pure("Service #1")
  }

  val two: ServiceTwo[IO] = new ServiceTwo[IO] {
    def get: IO[String] = IO.pure("Service #2")
  }

  val serviceModule: ServiceModule[IO] = ServiceModule[IO](one, two)

  implicit val serviceModuleReader: HasModule[IO] = moduleReader(serviceModule)

  def run(args: List[String]): IO[ExitCode] =
    Program[IO].as(ExitCode.Success)

}

case class ServiceModule[F[_]](
    serviceOne: ServiceOne[F],
    serviceTwo: ServiceTwo[F]
)

trait ServiceOne[F[_]] {
  def get: F[String]
}

trait ServiceTwo[F[_]] {
  def get: F[String]
}

object Program {
  def apply[F[_]: HasModule: Sync]: F[Unit] =
    for {
      m1 <- new ProgramOne[F].get
      m2 <- new ProgramTwo[F].get
      m3 <- new ProgramThree[F].get
      _ <- Sync[F].delay {
            println(s"M1: $m1")
            println(s"M2: $m2")
            println(s"M3: $m3")
          }
    } yield ()
}

class ProgramOne[F[_]: HasServiceOne: Monad] {
  def get: F[String] =
    ask[F, ServiceOne[F]].flatMap(_.get)
}

class ProgramTwo[F[_]: HasServiceTwo: Monad] {
  def get: F[String] =
    ask[F, ServiceTwo[F]].flatMap(_.get)
}

class ProgramThree[F[_]: HasServiceOne: HasServiceTwo: Monad] {
  def get: F[String] = {
    val fx = ask[F, ServiceOne[F]].flatMap(_.get)
    val fy = ask[F, ServiceTwo[F]].flatMap(_.get)
    (fx, fy).mapN { case (x, y) => x |+| " - " |+| y }
  }
}
