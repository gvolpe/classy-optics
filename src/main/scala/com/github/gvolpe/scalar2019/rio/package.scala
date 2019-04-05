package com.github.gvolpe.scalar2019

import cats.~>
import scalaz.zio._

package object rio {
  type RIO[-R, +A] = ZIO[R, Throwable, A] // TaskR[R, A]

  object RIO {
    def functionK[R](r: R): TaskR[R, ?] ~> Task = λ[TaskR[R, ?] ~> Task](_.provide(r))
  }
}
