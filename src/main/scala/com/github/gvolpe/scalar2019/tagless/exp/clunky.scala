package com.github.gvolpe.scalar2019.tagless.exp

import cats.syntax.all._
import com.olegpy.meow.hierarchy._
import cumbersome._
import scalaz.zio._
import scalaz.zio.interop.catz._

object clunky extends App {
  import com.github.gvolpe.scalar2019.rio.instances.mtl._

  // Natural transformation (~>) replaces `provide` in polymorphic code
  implicit val zfk = fk(new Graph[Task].appModule)

  def run(args: List[String]): UIO[Int] =
    Program
      .run[TaskR[AppModule[Task], ?], Task]
      .as(0)
      .orDie

}
