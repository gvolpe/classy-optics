package com.github.gvolpe.http

import algebra._
import cats.effect._
import cats.syntax.functor._
import com.github.gvolpe.http.domain.{ CatalogError, UserError }
import com.olegpy.meow.hierarchy._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      users <- interpreter.mkUserAlg[IO]
      catalog <- interpreter.mkCatalogAlg[IO]
      ec <- BlazeServerBuilder[IO]
             .bindHttp(8080, "0.0.0.0")
             .withHttpApp(new HttpServer[IO](users, catalog).httpApp)
             .serve
             .compile
             .drain
             .as(ExitCode.Success)
    } yield ec

}

class HttpServer[F[_]: Sync](
    users: UserAlg[F, UserError],
    catalog: CatalogAlg[F, CatalogError]
) {

  implicit val userErrorHandler    = UserHttpErrorHandler[F]
  implicit val catalogErrorHandler = CatalogHttpErrorHandler[F]

  val routes = new CoUserRoutesMTL[F](users, catalog).routes

  val httpApp: HttpApp[F] = routes.orNotFound

}
