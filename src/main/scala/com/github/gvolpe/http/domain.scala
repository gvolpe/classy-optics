package com.github.gvolpe.http

object domain {

  case class Item(name: String) extends AnyVal

  case class User(username: String, age: Int)
  case class UserUpdateAge(age: Int)

  sealed trait UserError extends Exception
  case class UserAlreadyExists(username: String) extends UserError
  case class UserNotFound(username: String) extends UserError
  case class InvalidUserAge(age: Int) extends UserError

  sealed trait CatalogError extends Exception
  case class ItemAlreadyExists(item: String) extends CatalogError
  case class CatalogNotFound(id: Long) extends CatalogError

}
