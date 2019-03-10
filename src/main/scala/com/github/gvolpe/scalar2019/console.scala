package com.github.gvolpe.scalar2019

import cats.effect.Sync

trait Console[F[_]] {
  def putStrLn[A](a: A): F[Unit]
}

object Console {
  def apply[F[_]](implicit ev: Console[F]): Console[F] = ev

  implicit def syncConsole[F[_]: Sync]: Console[F] =
    new Console[F] {
      def putStrLn[A](a: A): F[Unit] = Sync[F].delay(println(a))
    }
}
