package com.github.gvolpe.scalar2019.tagless

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl._
import module._

/*
 * Exploring the option of using the IO Monad instance of ApplicativeAsk to build
 * the dependency graph while abstracting over the effect type (tagless final).
 *
 * Creating an instance of `ApplicativeAsk` for a type that has no Reader effect
 * baked-in is indeed a hack but if you can live with it the benefits are great.
 * */
object catsapp extends IOApp {

  // Hacky instance
  def mkModuleReader[F[_]: Applicative](module: AppModule[F]): HasAppModule[F] =
    new DefaultApplicativeAsk[F, AppModule[F]] {
      override val applicative: Applicative[F] = implicitly
      override def ask: F[AppModule[F]]        = module.pure[F]
    }

  def run(args: List[String]): IO[ExitCode] =
    Graph
      .make[IO]
      .map(g => mkModuleReader(g.appModule))
      .flatMap { implicit m: HasAppModule[IO] =>
        Program.run[IO].as(ExitCode.Success)
      }

}
