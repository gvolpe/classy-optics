package com.github.gvolpe.scalar2019.tagless.alt

import cats.Functor
import cats.arrow.FunctionK
import laws._

object laws {
  final case class IsEq[A](lhs: A, rhs: A)

  implicit final class IsEqArrow[A](private val lhs: A) extends AnyVal {
    def <->(rhs: A): IsEq[A] = IsEq(lhs, rhs)
  }
}

trait GenReaderLaws[F[_], G[_], R] {
  def M: GenReader[F, G, R]

  def elimination[A](fa: F[A], env: R, ga: G[A]) =
    M.runReader(fa)(env) <-> ga

  def idempotency[A](fa: F[A], env: R, ga: G[A]) =
    M.runReader(M.unread(M.runReader(fa)(env)))(env) <-> M.runReader(fa)(env)
}

trait DependencyLaws {

  def identity[F[_]: Functor, A](fa: F[A])(implicit ev: Dependency[F, F]) =
    FunctionK.id[F](fa) <-> ev(fa)

  def composition[F[_], G[_], H[_], A](fa: F[A], ga: G[A])(
      implicit ev1: Dependency[F, G],
      ev2: Dependency[G, H]
  ) =
    ev2(ev1(fa)) <-> ev2(ga)

  def associativity[F[_], G[_], H[_], A](fa: F[A], ga: G[A], ha: H[A])(
      implicit ev1: Dependency[F, G],
      ev2: Dependency[G, H],
      ev3: Dependency[H, F]
  ) = {
    val f1 = (ev3[A] _) compose ((ev2[A] _) compose (ev1[A] _))
    val f2 = ((ev3[A] _) compose (ev2[A] _)) compose (ev1[A] _)
    f1(fa) <-> f2(fa)
  }

}
