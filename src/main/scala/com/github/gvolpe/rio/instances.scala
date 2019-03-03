package com.github.gvolpe.rio

import cats._
import cats.effect.{ Bracket, ExitCase, Sync }
import cats.mtl._
import scalaz.zio._
import scalaz.zio.interop.catz._

object instances extends RIOCatsEffect

private[rio] abstract class RIOCatsEffect extends RIOCatsCore with RIOCatsMtl {

  implicit def rioCatsSync[R]: Sync[RIO[R, ?]] =
    new RIOSync[R] { def F = Sync[Task] }

  abstract class RIOSync[R] extends RIOBracket[R] with Sync[RIO[R, ?]] {

    protected implicit override def F: Sync[Task]

    override def handleErrorWith[A](fa: RIO[R, A])(f: Throwable => RIO[R, A]): RIO[R, A] =
      ZIO.accessM { r =>
        F.suspend(F.handleErrorWith(fa.provide(r))(e => f(e).provide(r)))
      }

    override def flatMap[A, B](fa: RIO[R, A])(f: A => RIO[R, B]): RIO[R, B] =
      ZIO.accessM { r =>
        F.suspend(fa.provide(r).flatMap(f.andThen(_.provide(r))))
      }

    def suspend[A](thunk: => RIO[R, A]): RIO[R, A] =
      ZIO.accessM { r =>
        F.suspend(thunk.provide(r))
      }

    override def uncancelable[A](fa: RIO[R, A]): RIO[R, A] =
      ZIO.accessM { r =>
        F.suspend(F.uncancelable(fa.provide(r)))
      }
  }

  abstract class RIOBracket[R] extends Bracket[RIO[R, ?], Throwable] {

    protected implicit def F: Bracket[Task, Throwable]

    private[this] final val rioMonadError: MonadError[RIO[R, ?], Throwable] =
      new CatzMonadError[R] {}

    def pure[A](x: A): RIO[R, A] =
      rioMonadError.pure(x)

    def handleErrorWith[A](fa: RIO[R, A])(f: Throwable => RIO[R, A]): RIO[R, A] =
      rioMonadError.handleErrorWith(fa)(f)

    def raiseError[A](e: Throwable): RIO[R, A] =
      rioMonadError.raiseError(e)

    def flatMap[A, B](fa: RIO[R, A])(f: A => RIO[R, B]): RIO[R, B] =
      rioMonadError.flatMap(fa)(f)

    def tailRecM[A, B](a: A)(f: A => RIO[R, Either[A, B]]): RIO[R, B] =
      rioMonadError.tailRecM(a)(f)

    def bracketCase[A, B](
        acquire: RIO[R, A]
    )(use: A => RIO[R, B])(release: (A, ExitCase[Throwable]) => RIO[R, Unit]): RIO[R, B] =
      ZIO.accessM { r =>
        F.bracketCase(acquire.provide(r))(a => use(a).provide(r)) { (a, br) =>
          release(a, br).provide(r)
        }
      }

    override def uncancelable[A](fa: RIO[R, A]): RIO[R, A] =
      ZIO.accessM { r =>
        F.uncancelable(fa.provide(r))
      }
  }

}

private[rio] trait RIOCatsMtl { self: RIOCatsCore =>
  private def zioApplicative[R, E]: Applicative[ZIO[R, E, ?]] = new CatzMonad[R, E] {}

  implicit def zioApplicativeAsk[R, E]: ApplicativeAsk[ZIO[R, E, ?], R] =
    new ApplicativeAsk[ZIO[R, E, ?], R] {
      override val applicative: Applicative[ZIO[R, E, ?]] = zioApplicative[R, E]
      override def ask: ZIO[R, Nothing, R]                = ZIO.environment
      override def reader[A](f: R => A)                   = ask.map(f)
    }

}

private[rio] abstract class RIOCatsCore {

  private[rio] trait CatzFunctor[R, E] extends Functor[ZIO[R, E, ?]] {
    override final def map[A, B](fa: ZIO[R, E, A])(f: A => B): ZIO[R, E, B] = fa.map(f)
  }

  private[rio] trait CatzApplicative[R, E] extends Applicative[ZIO[R, E, ?]] {
    override final def pure[A](a: A): ZIO[R, E, A] = ZIO.succeed(a)
  }

  private[rio] trait CatzMonad[R, E] extends Monad[ZIO[R, E, ?]] with CatzApplicative[R, E] with CatzFunctor[R, E] {
    override final def flatMap[A, B](fa: ZIO[R, E, A])(f: A => ZIO[R, E, B]): ZIO[R, E, B] = fa.flatMap(f)
    override final def tailRecM[A, B](a: A)(f: A => ZIO[R, E, Either[A, B]]): ZIO[R, E, B] =
      f(a).flatMap {
        case Left(l)  => tailRecM(l)(f)
        case Right(r) => ZIO.succeed(r)
      }
  }

  private[rio] trait CatzMonadError[R] extends CatzMonad[R, Throwable] with MonadError[RIO[R, ?], Throwable] {
    override final def handleErrorWith[A](fa: RIO[R, A])(f: Throwable => RIO[R, A]): RIO[R, A] = fa.catchAll(f)
    override final def raiseError[A](e: Throwable): RIO[R, A]                                  = IO.fail(e)
  }

}
