
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql._

/** Represents a `User` object into the database
  * @param id the id of the user
  * @param email the user's email address
  * @param password the user's password (salted and hashed)
  * @param fname the user's first name
  * @param lname the user's last name
  * @param isAdmin true if the user is an admin
  */
case class User(
	id: Long,
	email: String,
	password: String, //hash+salt
	fname: String,
	lname: String,
	isAdmin: Boolean
)

/** The fields required to insert a new
  * `User` object into the database
  * @param email the user's email address
  * @param password the user's password (not hashed! do not store in this form!)
  * @param fname the user's first name
  * @param lname the user's last name
  * @param isAdmin true if the user should be an admin
  */
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

	/** Look up a user object by their email (verifing their password). Useful for login
	  * @param email the user's email
	  * @param password the user's password
	  * @param conn database connection to (re)use; one will be created null is received
	  * @throws SQLException the provided email/password combo doesn't match any in the database
	  */
	def apply(email: String, password: String)(implicit conn: Connection = null) = ensuringConnection { implicit conn => 
		val user = byEmail(email)
		if (!BCrypt.checkpw(password, user.password)) throw new SQLException("Invalid email or password")
		user
	}

	/** Look up a user object by their email address
	  * @param email the user's email
	  * @param conn database connection to (re)use; one will be created null is received
	  * @throws SQLException the provided email doesn't match any in the database
	  */
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

	/** Look up a user object by their (database) id
	  * @param id the user's id
	  * @param conn database connection to (re)use; one will be created null is received
	  * @throws SQLException the provided id doesn't match any in the database
	  */
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

	/** Retrieve a list of all users in the database
	  * @param id the user's id
	  * @param conn database connection to (re)use; one will be created null is received
	  */
	def all(implicit conn: Connection = null) = ensuringConnection { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM user;
			"""
		)

		mapUsers(query())
	}

	/** Retrieve a list of all users in the database
	  * @param user `UserTemplate` that describes the fields the inserted user should have
	  * @param trans database transaction to (re)use; one will be created null or a non-transaction (regular connection) is received
	  * @throws SQLException the insertion failed
	  */
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

	/** Change a user's password
	  * @param email email address, which scopes the password change request to a give user
	  * @param oldPass the user's current password (not hashed! don't store!)
	  * @param newPass the desired new password (not hashed! don't store in this form!)
	  * @param trans database transaction to (re)use; one will be created null or a non-transaction (regular connection) is received
	  * @throws SQLException the password change failed (probably due to incorrect `email` or `oldPass`)
	  */
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
