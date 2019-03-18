package com.github.gvolpe.scalar2019.tagless

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import com.olegpy.meow.hierarchy._

/*
 * These should use two different type constructors `F` and `G`. The first one
 * to represent the reader effect and the second one for the runnable effect.
 *
 * But as demonstrated in the `exp` package this makes the entire application
 * very cumbersome so we use a single one to represent both while acknowledging
 * the trade-offs.
 */
object module {
  type HasServiceOne[F[_]]    = ApplicativeAsk[F, ServiceOne[F]]
  type HasServiceTwo[F[_]]    = ApplicativeAsk[F, ServiceTwo[F]]
  type HasServiceModule[F[_]] = ApplicativeAsk[F, ServiceModule[F]]
  type HasUserDb[F[_]]        = ApplicativeAsk[F, UserDatabase[F]]
  type HasProfileDb[F[_]]     = ApplicativeAsk[F, ProfileDatabase[F]]
  type HasCache[F[_]]         = ApplicativeAsk[F, Cache[F]]
  type HasDbModule[F[_]]      = ApplicativeAsk[F, DatabaseModule[F]]
  type HasAppModule[F[_]]     = ApplicativeAsk[F, AppModule[F]]

  def ask[F[_], T[_[_]]](implicit ev: ApplicativeAsk[F, T[F]]) = ev.ask

  def putStrLn[F[_]: Sync, A](a: A): F[Unit] = Sync[F].delay(println(a))

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
    val userDb: UserDatabase[F] = deps.userDb.getOrElse(
      new UserDatabase[F] {
        def persist: F[Unit] = putStrLn("User db persist")
      }
    )

    val profileDb: ProfileDatabase[F] = deps.profileDb.getOrElse(
      new ProfileDatabase[F] {
        def persist: F[Unit] = putStrLn("Profile db persist")
      }
    )

    val cache: Cache[F] = new Cache[F] {
      def get: F[String] = "Cache data".pure[F]
    }

    val dbModule: DatabaseModule[F] =
      DatabaseModule[F](userDb, profileDb, cache)

    val one: ServiceOne[F] = new ServiceOne[F] {
      def get: F[String] = "Service #1".pure[F]
    }

    val two: ServiceTwo[F] = new ServiceTwo[F] {
      def get: F[String] = "Service #2".pure[F]
    }

    val serviceModule: ServiceModule[F] = ServiceModule[F](one, two)

    val appModule: AppModule[F] = AppModule[F](serviceModule, dbModule)
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
    (LiveProgramOne[F], LiveProgramTwo[F], LiveProgramThree[F]).mapN {
      case (p1, p2, p3) =>
        p1.get.flatMap(x => putStrLn(s"P1: $x")) *>
          p2.get.flatMap(x => putStrLn(s"P2: $x")) *>
          p3.get.flatMap(x => putStrLn(s"P3: $x"))
    }.flatten
}

object LiveProgramOne {
  def apply[F[_]: HasServiceOne: Monad]: F[ProgramOne[F]] =
    ask[F, ServiceOne].map(ProgramOne[F])
}

case class ProgramOne[F[_]: Monad](s1: ServiceOne[F]) {
  def get: F[String] = s1.get
}

object LiveProgramTwo {
  def apply[F[_]: HasCache: HasServiceTwo: Monad]: F[ProgramTwo[F]] =
    (ask[F, Cache], ask[F, ServiceTwo]).mapN(ProgramTwo[F])
}

case class ProgramTwo[F[_]: Monad](
    cache: Cache[F],
    s2: ServiceTwo[F]
) {
  def get: F[String] =
    (cache.get, s2.get).mapN { case (x, y) => x |+| " - " |+| y }
}

object LiveProgramThree {
  def apply[F[_]: HasServiceOne: HasServiceTwo: HasUserDb: Monad]: F[ProgramThree[F]] =
    (ask[F, ServiceOne], ask[F, ServiceTwo], ask[F, UserDatabase]).mapN(ProgramThree[F])
}

case class ProgramThree[F[_]: Monad](
    s1: ServiceOne[F],
    s2: ServiceTwo[F],
    db: UserDatabase[F]
) {
  def get: F[String] =
    (s1.get, s2.get, db.persist).mapN { case (x, y, _) => x |+| " - " |+| y }
}
