package com.github.gvolpe.http

import domain._
import cats.{ ApplicativeError, MonadError }
import cats.data.{ Kleisli, OptionT }
import cats.syntax.all._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object RoutesHttpErrorHandler {
  def apply[F[_]: ApplicativeError[?[_], E], E <: Throwable](
      routes: HttpRoutes[F]
  )(handler: E => F[Response[F]]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        routes.run(req).value.handleErrorWith(e => handler(e).map(Option(_)))
      }
    }
}

object HttpErrorHandler {
  def apply[F[_], E <: Throwable](implicit ev: HttpErrorHandler[F, E]) = ev
}

// -- instances

class UserHttpErrorHandler[F[_]: MonadError[?[_], UserError]] extends HttpErrorHandler[F, UserError] with Http4sDsl[F] {
  private val handler: UserError => F[Response[F]] = {
    case InvalidUserAge(age)         => BadRequest(s"Invalid age $age".asJson)
    case UserAlreadyExists(username) => Conflict(username.asJson)
    case UserNotFound(username)      => NotFound(username.asJson)
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}

class CatalogHttpErrorHandler[F[_]: MonadError[?[_], CatalogError]]
    extends HttpErrorHandler[F, CatalogError]
    with Http4sDsl[F] {
  private val handler: CatalogError => F[Response[F]] = {
    case ItemAlreadyExists(item) => Conflict(item.asJson)
    case CatalogNotFound(id)     => NotFound(id.asJson)
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}
