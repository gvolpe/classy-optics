package com.github.gvolpe.http

import algebra._
import domain._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import ErrorChannel.syntax._

object interpreter {

  def mkUserAlg[F[_]: Sync](implicit error: ErrorChannel[F, UserError]): F[UserAlg[F, UserError]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new UserAlg[F, UserError] {
        private def validateAge(age: Int): F[Unit] =
          if (age <= 0) error.raise(InvalidUserAge(age)) else ().pure[F]

        override def find(username: String): F[Option[User]] =
          state.get.map(_.get(username))

        override def save(user: User): F[Unit] =
          validateAge(user.age) *>
            find(user.username).flatMap {
              case Some(_) =>
                error.raise(UserAlreadyExists(user.username))
//                error.raise(new Exception("asd")) // Does not compile
//                Sync[F].raiseError(new Exception("")) // Should be considered an unrecoverable failure
              case None =>
                state.update(_.updated(user.username, user))
            }

        override def updateAge(username: String, age: Int): F[Unit] =
          validateAge(age) *>
            find(username).flatMap {
              case Some(user) =>
                state.update(_.updated(username, user.copy(age = age)))
              case None =>
                error.raise(UserNotFound(username))
            }
      }
    }

  def mkCatalogAlg[F[_]: ErrorChannel[?[_], CatalogError]: Sync]: F[CatalogAlg[F, CatalogError]] =
    Ref.of[F, Map[Long, List[Item]]](Map.empty).map { state =>
      new CatalogAlg[F, CatalogError] {
        override def find(id: Long): F[List[Item]] =
          state.get.map(_.get(id).toList.flatten)

        override def save(id: Long, item: Item): F[Unit] =
          find(id).flatMap {
            case _ :: _ =>
              ItemAlreadyExists(item.name).raise
            // error.raise(new Exception("asd")) // Does not compile
            // Sync[F].raiseError(new Exception("")) // Should be considered an unrecoverable failure
            case Nil =>
              state.update(st => st.updated(id, st.get(id).toList.flatten :+ item))
          }

      }
    }

}
