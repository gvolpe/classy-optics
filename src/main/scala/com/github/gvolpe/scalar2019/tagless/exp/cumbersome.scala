package com.github.gvolpe.scalar2019.tagless.exp

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import com.olegpy.meow.hierarchy._

/*
 * Exploring the option of using the RIO Monad instance of ApplicativeAsk to build
 * the dependency graph while abstracting over the effect type (tagless final).
 *
 * While possible, it involves working with two type constructors. The first one
 * representing the Reader effect and the second one the final effect without the
 * need of an environment / context.
 *
 * We also need a way to represent the relationship between F and G, here done using
 * natural transformation.
 * */
object cumbersome {
  type HasServiceModule[F[_], G[_]] = ApplicativeAsk[F, ServiceModule[G]]
  type HasDbModule[F[_], G[_]]      = ApplicativeAsk[F, DatabaseModule[G]]
  type HasAppModule[F[_], G[_]]     = ApplicativeAsk[F, AppModule[G]]

  def ask[F[_], G[_], T[_[_]]](implicit ev: ApplicativeAsk[F, T[G]], fk: F ~> G): G[T[G]] = fk(ev.ask)

  class Graph[F[_]: Sync] {
    def putStrLn[A](a: A): F[Unit] = Sync[F].delay(println(a))

    val dbModule: DatabaseModule[F]     = DatabaseModule[F]("foo".pure[F])
    val serviceModule: ServiceModule[F] = ServiceModule[F]("bar".pure[F])
    val appModule: AppModule[F]         = AppModule[F](serviceModule, dbModule)
  }

}

import cumbersome._

case class AppModule[F[_]](
    serviceModule: ServiceModule[F],
    databaseModule: DatabaseModule[F]
)

case class DatabaseModule[F[_]](foo: F[String])

case class ServiceModule[F[_]](bar: F[String])

object Program {
  def run[F[_]: Sync, G[_]: Sync](
      implicit ev: HasAppModule[F, G],
      fk: F ~> G
  ): G[Unit] =
    for {
      m1 <- new ProgramOne[F, G].get
      m2 <- new ProgramTwo[F, G].get
      m3 <- new ProgramThree[F, G].get
      _ <- Sync[G].delay {
            println(s"M1: $m1")
            println(s"M2: $m2")
            println(s"M3: $m3")
          }
    } yield ()
}

class ProgramOne[F[_]: Monad, G[_]: Monad](
    implicit ev: HasServiceModule[F, G],
    fk: F ~> G
) {
  def get: G[String] =
    ask[F, G, ServiceModule].flatMap(_.bar)
}

class ProgramTwo[F[_]: Monad, G[_]: Monad](
    implicit ev: HasDbModule[F, G],
    fk: F ~> G
) {
  def get: G[String] =
    ask[F, G, DatabaseModule].flatMap(_.foo)
}

class ProgramThree[F[_]: Monad, G[_]: Monad](
    implicit val ev1: HasServiceModule[F, G],
    implicit val ev2: HasDbModule[F, G],
    fk: F ~> G
) {
  def get: G[String] = {
    val fx = ask[F, G, ServiceModule].flatMap(_.bar)
    val fy = ask[F, G, DatabaseModule].flatMap(_.foo)
    (fx, fy).mapN { case (x, y) => x |+| " - " |+| y }
  }
}
