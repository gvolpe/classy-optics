package com.github.gvolpe.scalar2019
package classy

import cats._
import cats.effect._
import cats.implicits._
import com.olegpy.meow.hierarchy._

object monaderror extends IOApp {
  import errors._

  val h = new PaymentErrorHandler[IO]

  val p1 = IO.unit
  val p2 = IO.raiseError[Unit](InsufficientFunds("Balance: $0.47"))
  val p3 = IO.raiseError[Unit](PaymentRejected("Error: Confidential"))

  def run(args: List[String]): IO[ExitCode] =
    h.handle(p2).as(ExitCode.Success)

}

object errors {

  sealed trait PaymentError extends Exception
  case class InsufficientFunds(balance: String) extends PaymentError
  case class PaymentRejected(reason: String) extends PaymentError
  case class PaymentDuplicated(details: String) extends PaymentError

  trait ErrorHandler[F[_], E <: Throwable] {
    def handle(fa: F[Unit]): F[Unit]
  }

  class PaymentErrorHandler[F[_]: Console: MonadError[?[_], PaymentError]] extends ErrorHandler[F, PaymentError] {
    val handler: PaymentError => F[Unit] = {
      case InsufficientFunds(balance) => Console[F].putStrLn(balance)
      case PaymentRejected(reason)    => Console[F].putStrLn(reason)
      case PaymentDuplicated(details) => Console[F].putStrLn(details)
    }

    def handle(fa: F[Unit]): F[Unit] = fa.handleErrorWith(handler)
  }

}
