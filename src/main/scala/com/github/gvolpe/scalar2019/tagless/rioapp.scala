package com.github.gvolpe.scalar2019.tagless

import cats.syntax.all._
import com.olegpy.meow.hierarchy._
import module._
import scalaz.zio._
import scalaz.zio.interop.catz._

object rioapp extends App {
  import com.github.gvolpe.scalar2019.rio.instances.mtl._

  def run(args: List[String]): UIO[Int] =
    Graph
      .make[Task]()
      .flatMap { graph =>
        implicit val zfk = fk(graph.appModule) // TaskR[AppModule[Task], ?] ~> Task
        Program.run[Task]
      }
      .provide(Environment)
      .as(0)
      .orDie

  //import module.instances._
  //def run(args: List[String]): UIO[Int] =
  //  Graph
  //    .make[Task]()
  //    .map(g => moduleReader(g.appModule))
  //    .flatMap { implicit ev: HasAppModule[Task] =>
  //      Program.run[Task]
  //    }
  //    .provide(Environment)
  //    .as(0)
  //    .orDie

}
