package com.github.gvolpe.scalar2019
package classy

import cats._
import cats.effect._
import cats.implicits._
import com.olegpy.meow.hierarchy._

object monaderror extends IOApp {
  import errors._

  val h = PaymentErrorHandler[IO]

  val p1 = IO.unit
  val p2 = IO.raiseError[Unit](InsufficientFunds("Balance: $0.47"))
  val p3 = IO.raiseError[Unit](PaymentRejected("Error: Confidential"))

  def run(args: List[String]): IO[ExitCode] =
    h.handleErrors(p2).as(ExitCode.Success)

}

object errors {

  sealed trait PaymentError extends Exception
  case class InsufficientFunds(balance: String) extends PaymentError
  case class PaymentRejected(reason: String) extends PaymentError
  case class PaymentDuplicated(details: String) extends PaymentError

  abstract class ErrorHandler[F[_], E <: Throwable, A] {
    def M: MonadError[F, E]
    def handler: E => F[A]
    def handleErrors(fa: F[A]): F[A] = M.handleErrorWith(fa)(handler)
  }

  object PaymentErrorHandler {
    def apply[F[_]: Console: MonadError[?[_], PaymentError]]: ErrorHandler[F, PaymentError, Unit] =
      new ErrorHandler[F, PaymentError, Unit] {
        val M = implicitly

        val handler: PaymentError => F[Unit] = {
          case InsufficientFunds(balance) => Console[F].putStrLn(balance)
          case PaymentRejected(reason)    => Console[F].putStrLn(reason)
          case PaymentDuplicated(details) => Console[F].putStrLn(details)
        }
      }
  }

}
