package com.github.gvolpe

import scalaz.zio.ZIO

package object rio {
  type RIO[-R, +A] = ZIO[R, Throwable, A]
}
