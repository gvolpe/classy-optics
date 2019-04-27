package com.github.gvolpe.scalar2019.tagless

import cats.syntax.all._
import com.olegpy.meow.hierarchy._
import module._
import scalaz.zio._
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._

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
 * This relationship can be represented as a `Dependency[F, G]` which can be seen as
 * a especialized case of natural transformation with different laws.
 *
 * We can only create a `Dependency` by introducing a generalized reader typeclass
 * that abstracts over `provide`: `GenReader[F, G, R]`. For example, an instance:
 *
 * - `GenReader[TaskR[R, ?], Task, R]`
 *
 * So given instances of
 *
 * - `ApplicativeAsk[TaskR[R, ?], R]` and
 * - `Dependency[TaskR[R, ?], Task]`
 *
 * we can automatically derive an instance of `ApplicativeAsk[Task, R]` and make
 * direct use of `Task` to construct our program.
 * */
object rioapp extends App {
  import alt._
  import alt.instances.deps._
  import alt.instances.mtl._

  type Dep[F[_[_]]] = Dependency[TaskR[F[Task], ?], Task]

  val mkGraph: Task[Dep[AppModule]] =
    Dependency.make(Graph.make[Task].map(_.appModule))

  def run(args: List[String]): UIO[Int] =
    mkGraph
      .flatMap { implicit dep =>
        Program.run[Task]
      }
      .as(0)
      .orDie

}
