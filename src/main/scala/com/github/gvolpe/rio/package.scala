package com.github.gvolpe

import scalaz.zio._

package object rio {
  type RIO[-R, +A] = TaskR[R, A]
}
