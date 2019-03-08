package com.github.gvolpe.scalar2019.tagless

import cats.effect._
import cats.implicits._
import module._
import module.instances._

object catsapp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Graph
      .make[IO]()
      .map(g => moduleReader(g.appModule))
      .flatMap { implicit m: HasAppModule[IO] =>
        Program.run[IO].as(ExitCode.Success)
      }

}
