package com.github.gvolpe.http

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.http.algebra._
import com.github.gvolpe.http.domain._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._

abstract class UserRoutes[F[_]: HttpErrorHandler[?[_], E], E <: Throwable](
    users: UserAlg[F, E]
) extends Routes[F, E]

class UserRoutesAlt[F[_]: HttpErrorHandler[?[_], UserError]: Sync](
    users: UserAlg[F, UserError]
) extends UserRoutes(users) {

  private[http] val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      users.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None       => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req
        .as[User]
        .flatMap { user =>
          users.save(user) *> Created(user.username.asJson)
        }

    case req @ PUT -> Root / "users" / username =>
      req
        .as[UserUpdateAge]
        .flatMap { userUpdate =>
          users.updateAge(username, userUpdate.age) *> Ok(username.asJson)
        }
  }

}
