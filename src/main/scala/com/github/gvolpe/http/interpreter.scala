package com.github.gvolpe.http

import algebra._
import domain._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

object interpreter {

  def create[F[_]](implicit F: Sync[F]): F[UserAlg[F]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new UserAlg[F] {
        private def validateAge(age: Int): F[Unit] =
          if (age <= 0) F.raiseError(InvalidUserAge(age)) else F.unit

        override def find(username: String): F[Option[User]] =
          state.get.map(_.get(username))

        override def save(user: User): F[Unit] =
          validateAge(user.age) *>
            find(user.username).flatMap {
              case Some(_) =>
                F.raiseError(UserAlreadyExists(user.username))
              case None =>
                state.update(_.updated(user.username, user))
            }

        override def updateAge(username: String, age: Int): F[Unit] =
          validateAge(age) *>
            find(username).flatMap {
              case Some(user) =>
                state.update(_.updated(username, user.copy(age = age)))
              case None =>
                F.raiseError(UserNotFound(username))
            }
      }
    }

}
