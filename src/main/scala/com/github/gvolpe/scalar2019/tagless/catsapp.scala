package com.github.gvolpe.scalar2019.tagless

import cats.effect._
import cats.implicits._
import module._
import module.instances._

/*
 * Exploring the option of using the IO Monad instance of ApplicativeAsk to build
 * the dependency graph while abstracting over the effect type (tagless final).
 *
 * Creating an instance of `ApplicativeAsk` for a type that has no Reader effect
 * baked-in is indeed a hack but if you can live with it the benefits are great.
 * */
object catsapp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Graph
      .make[IO]()
      .map(g => moduleReader(g.appModule))
      .flatMap { implicit m: HasAppModule[IO] =>
        Program.run[IO].as(ExitCode.Success)
      }

}
