
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql._

case class User(
	id: Int,
	name: String,
	isAdmin: Boolean
)

case class UserTemplate(
	name: String,
	isAdmin: Boolean
)

object User {
	def all(implicit conn: Connection = null) = ensuringConnection { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM user;
			"""
		)

		val rows = query()

		rows.map { row =>
			User(
				row[Int]("id"),
				row[String]("username"),
				row[Boolean]("admin")
			)
		} toList
	}

	def insert(user: UserTemplate)(implicit trans: Connection = null) = ensuringTransaction { implicit conn =>
		val query = SQL(
			"""
				INSERT INTO user (username, admin)
				VALUES ({name}, {admin})
			""").on(
				"name" -> user.name,
				"admin" -> user.isAdmin
			)

			query.executeInsert().getOrElse(throw new SQLException("Failed to insert user"))
	}
}
