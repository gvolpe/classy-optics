package com.github.gvolpe.sbtb2018.http

import algebra._
import domain._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

case class UserRoutesMTL[F[_]: Sync](users: UserAlg[F]) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" / username =>
      users.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None       => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        users.save(user) *> Created(user.username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        users.updateAge(username, userUpdate.age) *> Created(username.asJson)
      }
  }

  def routes(implicit H: HttpErrorHandler[F, UserError]): HttpRoutes[F] =
    H.handle(httpRoutes)

}
