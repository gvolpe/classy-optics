package com.github.gvolpe.scalar2019.tagless

import cats.syntax.all._
import com.github.gvolpe.scalar2019.rio._
import com.olegpy.meow.hierarchy._
import module._
import scalaz.zio._
import scalaz.zio.interop.catz._

/*
 * Exploring the option of using the RIO Monad instance of ApplicativeAsk to build
 * the dependency graph while abstracting over the effect type (tagless final).
 *
 * Here we are using a single type constructor to represent both the Reader effect
 * and the final effect to make it easier but these two are by nature different: the
 * first one needs an "environment" before it can be executed.
 *
 * But if we look at the concrete type `TaskR[R, A]`, in order to get a runnable
 * `Task[A]` all we need to do is call `provide(r)` with the environmental R.
 *
 * This relationship can be represented using natural transformation `F ~> G`.
 *
 * So given instances of `ApplicativeAsk[TaskR[R, ?], R]` and `TaskR[R, ?] ~> Task`
 * we can automatically derive an instance of `ApplicativeAsk[Task, R]` and make
 * direct use of `Task` to construct our program.
 * */
object rioapp extends App {
  import com.github.gvolpe.scalar2019.rio.instances.mtl._

  // Natural transformation (~>) replaces `provide` in polymorphic code
  def run(args: List[String]): UIO[Int] =
    Graph
      .make[Task]()
      .flatMap { graph =>
        implicit val fk = RIO.functionK(graph.appModule) // TaskR[AppModule[Task], ?] ~> Task
        Program.run[Task]
      }
      .as(0)
      .orDie

  /*
 * We could build the `ApplicativeAsk` instance manually as we do with `cats.effect.IO` but
 * it feels more hacky than having a clear relationship represented with `F ~> G`.
 *
 *import module.instances._
 *def run(args: List[String]): UIO[Int] =
 *  Graph
 *    .make[Task]()
 *    .map(g => mkModuleReader(g.appModule))
 *    .flatMap { implicit ev: HasAppModule[Task] =>
 *      Program.run[Task]
 *    }
 *    .as(0)
 *    .orDie
 */

}
