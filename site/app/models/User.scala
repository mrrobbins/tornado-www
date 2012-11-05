
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql._

case class User(
	id: Long,
	email: String,
	password: String, //hash+salt
	fname: String,
	lname: String,
	isAdmin: Boolean
)

case class UserTemplate(
	email: String,
	password: String, //clear text
	fname: String,
	lname: String,
	isAdmin: Boolean
)

object User {
	private def mapUsers(rows: Stream[Row]): List[User] = {
		rows.map { row => 
			User(
				row[Long]("id"),
				row[String]("email"),
				row[String]("password"),
				row[String]("fname"),
				row[String]("lname"),
				row[Boolean]("admin")
			)
		}.toList
	}

	def apply(email: String, password: String)(implicit conn: Connection = null) = ensuringConnection { implicit conn => 
		val user = byEmail(email)
		if (!BCrypt.checkpw(password, user.password)) throw new SQLException("Invalid email or password")
		user
	}

	def byEmail(email: String)(implicit conn: Connection = null) = ensuringConnection { implicit conn =>
		val query = SQL (
			""" 
				SELECT * FROM user WHERE email = {email}
			"""
		).on(
			"email" -> email
		)

		val users = mapUsers(query())
		assert((0 to 1).contains(users.length), "Duplicate email in database")
		users.headOption.getOrElse(throw new SQLException("No user with email"))
	}

	def byId(id: Long)(implicit conn: Connection = null) = ensuringConnection { implicit conn =>
		val query = SQL (
			""" 
				SELECT * FROM user WHERE id = {id}
			"""
		).on(
			"id" -> id 
		)

		val users = mapUsers(query())
		assert((0 to 1).contains(users.length), "Duplicate id in database")
		users.headOption.getOrElse(throw new SQLException("No user with id"))
	}

	def all(implicit conn: Connection = null) = ensuringConnection { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM user;
			"""
		)

		mapUsers(query())
	}

	def insert(user: UserTemplate)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val hashSaltPassword = BCrypt.hashpw(user.password, BCrypt.gensalt(5))

		val query = SQL(
			"""
				INSERT INTO user (email, password, fname, lname, admin)
				VALUES ({email}, {password}, {fname}, {lname}, {admin})
			"""
		).on(
			"email" -> user.email,
			"password" -> hashSaltPassword,
			"fname" -> user.fname,
			"lname" -> user.lname,
			"admin" -> user.isAdmin
		)

			query.executeInsert().getOrElse(throw new SQLException("Failed to insert user"))
	}

	def changePassword(email: String, oldPass: String, newPass: String)(implicit trans: Connection = null) = ensuringTransaction { implicit trans => 
		val user = User(email, oldPass) 
		val newHashSaltPassword = BCrypt.hashpw(newPass, BCrypt.gensalt(5))
		val query = SQL(
			"""
				UPDATE user SET password = {newPass} WHERE email = {email} AND password = {oldPass}
			"""
		).on(
			"email" -> user.email,
			"oldPass" -> user.password,
			"newPass" -> newHashSaltPassword
		)

		if (query.executeUpdate() != 1) throw new SQLException("Failed to change password")
	}

}
