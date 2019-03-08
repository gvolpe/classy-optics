package com.github.gvolpe.scalar2019.tagless

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

  case class Rewritable[F[_]] private (
      userDb: Option[UserDatabase[F]] = None,
      profileDb: Option[ProfileDatabase[F]] = None
  )

  object Rewritable {
    def empty[F[_]]: Rewritable[F] = new Rewritable[F]()
  }

  object Graph {
    def make[F[_]: Sync](deps: Rewritable[F] = Rewritable.empty[F]): F[Graph[F]] =
      new Graph[F](deps).pure[F] // This is normally an effectful operation
  }

  class Graph[F[_]: Sync](deps: Rewritable[F]) {
    def putStrLn[A](a: A): F[Unit] = Sync[F].delay(println(a))

    val userDb: UserDatabase[F] = new UserDatabase[F] {
      def persist: F[Unit] = putStrLn("User db persist")
    }

    val profileDb: ProfileDatabase[F] = new ProfileDatabase[F] {
      def persist: F[Unit] = putStrLn("Profile db persist")
    }

    val cache: Cache[F] = new Cache[F] {
      def get: F[String] = "Cache data".pure[F]
    }

    val dbModule: DatabaseModule[F] =
      DatabaseModule[F](deps.userDb.getOrElse(userDb), deps.profileDb.getOrElse(profileDb), cache)

    val one: ServiceOne[F] = new ServiceOne[F] {
      def get: F[String] = "Service #1".pure[F]
    }

    val two: ServiceTwo[F] = new ServiceTwo[F] {
      def get: F[String] = "Service #2".pure[F]
    }

    val serviceModule: ServiceModule[F] = ServiceModule[F](one, two)

    val appModule: AppModule[F] = AppModule[F](serviceModule, dbModule)
  }

  object instances {
    def moduleReader[F[_]: Applicative](module: AppModule[F]): HasAppModule[F] =
      new DefaultApplicativeAsk[F, AppModule[F]] {
        override val applicative: Applicative[F] = implicitly
        override def ask: F[AppModule[F]]        = module.pure[F]
      }
  }
}

import module._

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
  def run[F[_]: HasAppModule: Sync]: F[Unit] =
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
