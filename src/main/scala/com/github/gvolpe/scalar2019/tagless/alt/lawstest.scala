package com.github.gvolpe.scalar2019.tagless.alt

import cats._
import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import scalaz.zio.{ DefaultRuntime, Runtime, Task, TaskR, ZIO }
import scalaz.zio.interop.catz._

object lawstest extends App with CheckLaws with EqInstances {
  import instances.deps._

  implicit val runtime = new DefaultRuntime {}

  val env: String = "ctx"

  val ga1: Id[Int]   = 123
  val ga2: IO[Int]   = IO.pure(123)
  val ga3: Task[Int] = 123.pure[Task]

  val fa1: Kleisli[Id, String, Int] = Kleisli(_ => ga1)
  val fa2: Kleisli[IO, String, Int] = Kleisli(_ => ga2)
  val fa3: TaskR[String, Int]       = ZIO.accessM(_ => ga3)

  check(KleisliGenReaderLaws[Id, String].elimination(fa1, env, ga1))
  check(KleisliGenReaderLaws[IO, String].elimination(fa2, env, ga2))
  check(TaskRGenReaderLaws[String].elimination(fa3, env, ga3))

  println("✔️  All tests have passed! (•̀ᴗ•́)و ̑̑")

}

trait CheckLaws {
  def check[A: Eq](results: laws.IsEq[A]*): Unit =
    results.foreach { rs =>
      try {
        assert(Eq[A].eqv(rs.lhs, rs.rhs), s"${rs.lhs} was not equals to ${rs.rhs}")
      } catch {
        case e: AssertionError =>
          System.err.println(s"💥💥💥 ${e.getMessage} 💥💥💥")
          System.exit(-1)
      }
    }
}

trait EqInstances {

  implicit def eqIO[A: Eq]: Eq[IO[A]] =
    new Eq[IO[A]] {
      def eqv(x: IO[A], y: IO[A]): Boolean =
        Eq[A].eqv(x.unsafeRunSync(), y.unsafeRunSync())
    }

  implicit def eqTask[A: Eq](implicit rts: Runtime[Any]): Eq[Task[A]] =
    new Eq[Task[A]] {
      def eqv(x: Task[A], y: Task[A]): Boolean =
        Eq[A].eqv(rts.unsafeRun(x), rts.unsafeRun(y))
    }

}

object KleisliGenReaderLaws {
  def apply[F[_], R](implicit ev: TransReader[Kleisli, F, R]): GenReaderLaws[Kleisli[F, R, ?], F, R] =
    new GenReaderLaws[Kleisli[F, R, ?], F, R] {
      val M = new GenReader[Kleisli[F, R, ?], F, R] {
        def apply[A]: Kleisli[F, R, A] => R => F[A] = ev.apply
      }
    }
}

object TaskRGenReaderLaws {
  def apply[R](implicit ev: BiReader[TaskR, R]): GenReaderLaws[TaskR[R, ?], Task, R] =
    new GenReaderLaws[TaskR[R, ?], Task, R] {
      val M = new GenReader[TaskR[R, ?], Task, R] {
        def apply[A]: TaskR[R, A] => R => Task[A] = ev.apply
      }
    }
}
