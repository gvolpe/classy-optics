package com.github.gvolpe.scalar2019

import scalaz.zio.ZIO

package object rio {
  type RIO[-R, +A] = ZIO[R, Throwable, A]
}
