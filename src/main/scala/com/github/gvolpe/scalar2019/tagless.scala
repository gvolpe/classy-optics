package com.github.gvolpe.scalar2019

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import com.olegpy.meow.hierarchy._

object module {
  type HasServiceOne[F[_]]    = ApplicativeAsk[F, ServiceOne[F]]
  type HasServiceTwo[F[_]]    = ApplicativeAsk[F, ServiceTwo[F]]
  type HasServiceModule[F[_]] = ApplicativeAsk[F, ServiceModule[F]]
  type HasUserDb[F[_]]        = ApplicativeAsk[F, UserDatabase[F]]
  type HasProfileDb[F[_]]     = ApplicativeAsk[F, ProfileDatabase[F]]
  type HasCache[F[_]]         = ApplicativeAsk[F, Cache[F]]
  type HasDbModule[F[_]]      = ApplicativeAsk[F, DatabaseModule[F]]
  type HasAppModule[F[_]]     = ApplicativeAsk[F, AppModule[F]]

  def ask[F[_], T](implicit ev: ApplicativeAsk[F, T]) = ev.ask

  object instances {
    def moduleReader(module: AppModule[IO]): HasAppModule[IO] =
      new DefaultApplicativeAsk[IO, AppModule[IO]] {
        override val applicative: Applicative[IO] = implicitly
        override def ask: IO[AppModule[IO]]       = IO.pure(module)
      }

    val userDb: UserDatabase[IO] = new UserDatabase[IO] {
      def persist: IO[Unit] = IO(println("User db persist"))
    }

    val profileDb: ProfileDatabase[IO] = new ProfileDatabase[IO] {
      def persist: IO[Unit] = IO(println("Profile db persist"))
    }

    val cache: Cache[IO] = new Cache[IO] {
      def get: IO[String] = IO.pure("Cache data")
    }

    val dbModule: DatabaseModule[IO] = DatabaseModule[IO](userDb, profileDb, cache)

    val one: ServiceOne[IO] = new ServiceOne[IO] {
      def get: IO[String] = IO.pure("Service #1")
    }

    val two: ServiceTwo[IO] = new ServiceTwo[IO] {
      def get: IO[String] = IO.pure("Service #2")
    }

    val serviceModule: ServiceModule[IO] = ServiceModule[IO](one, two)

    val appModule: AppModule[IO] = AppModule[IO](serviceModule, dbModule)
  }
}

import module._

object tagless extends IOApp {
  import module.instances._

  implicit val appModuleReader: HasAppModule[IO] = moduleReader(appModule)

  def run(args: List[String]): IO[ExitCode] =
    Program[IO].as(ExitCode.Success)

}

case class AppModule[F[_]](
    serviceModule: ServiceModule[F],
    databaseModule: DatabaseModule[F]
)

case class DatabaseModule[F[_]](
    userDb: UserDatabase[F],
    profileDb: ProfileDatabase[F],
    cache: Cache[F]
)

case class ServiceModule[F[_]](
    serviceOne: ServiceOne[F],
    serviceTwo: ServiceTwo[F]
)

trait UserDatabase[F[_]] {
  def persist: F[Unit]
}

trait ProfileDatabase[F[_]] {
  def persist: F[Unit]
}

trait Cache[F[_]] {
  def get: F[String]
}

trait ServiceOne[F[_]] {
  def get: F[String]
}

trait ServiceTwo[F[_]] {
  def get: F[String]
}

object Program {
  def apply[F[_]: HasAppModule: Sync]: F[Unit] =
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

class ProgramTwo[F[_]: HasCache: HasServiceTwo: Monad] {
  def get: F[String] = {
    val fx = ask[F, ServiceTwo[F]].flatMap(_.get)
    val fy = ask[F, Cache[F]].flatMap(_.get)
    (fx, fy).mapN { case (x, y) => x |+| " - " |+| y }
  }
}

class ProgramThree[F[_]: HasUserDb: HasServiceOne: HasServiceTwo: Monad] {
  def get: F[String] = {
    val fx = ask[F, ServiceOne[F]].flatMap(_.get)
    val fy = ask[F, ServiceTwo[F]].flatMap(_.get)
    val fz = ask[F, UserDatabase[F]].flatMap(_.persist)
    (fx, fy, fz).mapN { case (x, y, _) => x |+| " - " |+| y }
  }
}
