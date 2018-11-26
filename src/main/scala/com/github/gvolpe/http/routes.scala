package com.github.gvolpe.http

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import shapeless.Coproduct

abstract class Routes[F[_], E <: Throwable](implicit H: HttpErrorHandler[F, E]) extends Http4sDsl[F] {
  private[http] def httpRoutes: HttpRoutes[F]
  val routes: HttpRoutes[F] = H.handle(httpRoutes)
}

abstract class CoRoutes[F[_], E <: Coproduct](implicit CH: CoHttpErrorHandler[F, E]) extends Http4sDsl[F] {
  private[http] def httpRoutes: HttpRoutes[F]
  val routes: HttpRoutes[F] = CH.handle(httpRoutes)
}
