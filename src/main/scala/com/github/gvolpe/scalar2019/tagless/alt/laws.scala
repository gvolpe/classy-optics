package com.github.gvolpe.scalar2019.tagless.alt

import laws._

object laws {
  final case class IsEq[A](lhs: A, rhs: A)

  implicit final class IsEqArrow[A](private val lhs: A) extends AnyVal {
    def <->(rhs: A): IsEq[A] = IsEq(lhs, rhs)
  }
}

trait MkDepLaws[F[- _, _], R] {
  def M: MkDep[F, R]

  /*
   * Feeding R to F[R, A] eliminates R ang gives you F[Any, A] which is just F[A]
   */
  def elimination[A](fra: F[R, A], env: R, fa: F[Any, A]) = M[A](fra)(env) <-> fa

}

trait DependencyLaws {

  def identity[F[_], A](fa: F[A])(implicit ev: Dependency[F, F]) =
    ev.apply(fa) <-> fa

  def composition[F[_], G[_], H[_], A](fa: F[A], ga: G[A])(
      implicit ev1: Dependency[F, G],
      ev2: Dependency[G, H]
  ) =
    ev2(ev1(fa)) <-> ev2(ga)

}
