package com.github.gvolpe.http

import cats.ApplicativeError
import domain._

object algebra {

  trait ErrorChannel[F[_], E <: Throwable] {
    def raise[A](e: E): F[A]
  }

  object ErrorChannel {
    def apply[F[_], E <: Throwable](implicit ev: ErrorChannel[F, E]) = ev

    implicit def instance[F[_], E <: Throwable](implicit F: ApplicativeError[F, Throwable]): ErrorChannel[F, E] =
      new ErrorChannel[F, E] {
        override def raise[A](e: E) = F.raiseError(e)
      }

    object syntax {
      implicit class ErrorChannelOps[F[_]: ErrorChannel[?[_], E], E <: Throwable](e: E) {
        def raise[A]: F[A] = ErrorChannel[F, E].raise[A](e)
      }
    }
  }

  abstract class UserAlg[F[_]: ErrorChannel[?[_], E], E <: Throwable] {
    def find(username: String): F[Option[User]]
    def save(user: User): F[Unit]
    def updateAge(username: String, age: Int): F[Unit]
  }

  abstract class CatalogAlg[F[_]: ErrorChannel[?[_], E], E <: Throwable] {
    def find(id: Long): F[List[Item]]
    def save(id: Long, item: Item): F[Unit]
  }

}
