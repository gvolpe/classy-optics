package com.github.gvolpe.sbtb2018.http

import domain._
import cats.{ ApplicativeError, MonadError }
import cats.data.{ Kleisli, OptionT }
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

abstract class RoutesHttpErrorHandler[F[_], E <: Throwable] extends HttpErrorHandler[F, E] with Http4sDsl[F] {
  def A: ApplicativeError[F, E]
  def handler: E => F[Response[F]]
  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        A.handleErrorWith(routes.run(req).value)(e => A.map(handler(e))(Option(_)))
      }
    }
}

object HttpErrorHandler {
  @inline final def apply[F[_], E <: Throwable](implicit ev: HttpErrorHandler[F, E]) = ev
}

// -- instances

object UserHttpErrorHandler {
  def apply[F[_]: MonadError[?[_], UserError]]: HttpErrorHandler[F, UserError] =
    new RoutesHttpErrorHandler[F, UserError] {
      val A = implicitly

      val handler: UserError => F[Response[F]] = {
        case InvalidUserAge(age)         => BadRequest(s"Invalid age $age".asJson)
        case UserAlreadyExists(username) => Conflict(username.asJson)
        case UserNotFound(username)      => NotFound(username.asJson)
      }
    }
}
