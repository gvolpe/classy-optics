package com.github.gvolpe.scalar2019

import scalaz.zio._

package object rio {
  type RIO[-R, +A] = TaskR[R, A]
}
