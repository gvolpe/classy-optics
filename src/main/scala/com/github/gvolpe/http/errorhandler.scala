package com.github.gvolpe.http

import domain._
import cats.{ ApplicativeError, MonadError }
import cats.data.{ Kleisli, OptionT }
import cats.syntax.all._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless._

/***
  * Typeclass for custom error handling: specifically mapping business errors to Http Responses.
  */
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

  def mkInstance[F[_]: MonadError[?[_], E], E <: Throwable](
      handler: E => F[Response[F]]
  ): HttpErrorHandler[F, E] =
    (routes: HttpRoutes[F]) => RoutesHttpErrorHandler(routes)(handler)
}

/**
  * Typeclass for customer error handling operating over a co-product of error types.
  * */
trait CHttpErrorHandler[F[_], Err <: Coproduct] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object CHttpErrorHandler {
  def apply[F[_], Err <: Coproduct](implicit ev: CHttpErrorHandler[F, Err]) = ev

  implicit def cNilInstance[F[_]]: CHttpErrorHandler[F, CNil] =
    (routes: HttpRoutes[F]) => routes

  implicit def consInstance[F[_], E <: Throwable, T <: Coproduct](
      implicit H: HttpErrorHandler[F, E],
      CH: CHttpErrorHandler[F, T]
  ): CHttpErrorHandler[F, :+:[E, T]] =
    (routes: HttpRoutes[F]) => CH.handle(H.handle(routes))
}

// ----------- instances -------------

object UserHttpErrorHandler {
  def apply[F[_]: MonadError[?[_], UserError]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    HttpErrorHandler.mkInstance[F, UserError] {
      case InvalidUserAge(age)         => BadRequest(s"Invalid age $age".asJson)
      case UserAlreadyExists(username) => Conflict(username.asJson)
      case UserNotFound(username)      => NotFound(username.asJson)
    }
  }
}

object CatalogHttpErrorHandler {
  def apply[F[_]: MonadError[?[_], CatalogError]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    HttpErrorHandler.mkInstance[F, CatalogError] {
      case ItemAlreadyExists(item) => Conflict(item.asJson)
      case CatalogNotFound(id)     => NotFound(id.asJson)
    }
  }
}
