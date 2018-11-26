package com.github.gvolpe.http

import algebra._
import domain._
import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.http.CoUserRoutes.CustomError
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import shapeless._

abstract class CoUserRoutes[
    F[_]: CoHttpErrorHandler[?[_], E],
    A <: Throwable,
    B <: Throwable,
    E <: Coproduct: =:=[?, A :+: B :+: CNil]
](
    users: UserAlg[F, A],
    catalog: CatalogAlg[F, B]
) extends CoRoutes[F, E]

object CoUserRoutes {
  type CustomError = UserError :+: CatalogError :+: CNil
}

class CoUserRoutesMTL[F[_]: CoHttpErrorHandler[?[_], CustomError]: Sync](
    users: UserAlg[F, UserError],
    catalog: CatalogAlg[F, CatalogError]
) extends CoUserRoutes(users, catalog) {

  private[http] val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "catalogs" =>
      Ok(catalog.find(23).map(_.asJson))

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

}
