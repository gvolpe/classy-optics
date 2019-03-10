package com.github.gvolpe.scalar2019.tagless.exp

import com.github.gvolpe.scalar2019.rio._
import com.olegpy.meow.hierarchy._
import cumbersome._
import scalaz.zio._
import scalaz.zio.interop.catz._

object clunky extends App {
  import com.github.gvolpe.scalar2019.rio.instances.mtl._

  // Natural transformation (~>) replaces `provide` in polymorphic code
  implicit val fk = RIO.functionK(new Graph[Task].appModule)

  def run(args: List[String]): UIO[Int] =
    Program
      .run[TaskR[AppModule[Task], ?], Task]
      .either
      .map(_.fold(_ => 1, _ => 0))

}
