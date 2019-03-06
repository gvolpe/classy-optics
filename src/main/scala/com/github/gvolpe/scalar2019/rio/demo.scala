package com.github.gvolpe.scalar2019.rio

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.sbtb2018.config._
import com.github.gvolpe.sbtb2018._
import com.olegpy.meow.hierarchy._

import scalaz.zio._
import scalaz.zio.interop.catz._

object demo extends App {
  import instances.mtl._

  override def run(args: List[String]): UIO[Int] =
    loadConfig[Task]
      .flatMap { config =>
        new RIOMain[RIO[AppConfig, ?]].foo.provide(config)
      }
      .provide(Environment)
      .as(0)
      .orDie

}

class RIOMain[F[_]: HasAppConfig: Sync] {

  val p1 = new ProgramOne[F]
  val p2 = new ProgramTwo[F]
  val p3 = new ProgramThree[F]

  def foo: F[Unit] =
    p1.foo *> p2.foo *> p3.foo

}
