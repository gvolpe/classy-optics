package com.github.gvolpe.rio

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.sbtb2018.config._
import com.github.gvolpe.sbtb2018._
import com.olegpy.meow.hierarchy._

import scalaz.zio._

object demo extends App {
  import instances._

  override def run(args: List[String]): UIO[Int] =
    loadConfig[Task]
      .flatMap { config =>
        new RIOMain[RIO[AppConfig, ?]].foo.provide(config)
      }
      .orDie
      .provide(Environment)
      .map(_ => 0)

}

class RIOMain[F[_]: HasAppConfig: Sync] {

  val p1 = new ProgramOne[F]
  val p2 = new ProgramTwo[F]
  val p3 = new ProgramThree[F]

  def foo: F[Unit] =
    p1.foo *> p2.foo *> p3.foo

}
