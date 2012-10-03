
package models

import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException

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
	def all = DB.withConnection { conn =>
		val query = SQL(
			"""
				SELECT * FROM user;
			"""
		)

		val rows = query()(conn)

		rows.map { row =>
			User(
				row[Int]("id"),
				row[String]("username"),
				row[Boolean]("admin")
			)
		} toList
	}

	def insert(user: UserTemplate) = DB.withConnection { conn =>
		val query = SQL(
			"""
				INSERT INTO user (username, admin)
				VALUES ({name}, {admin})
			""").on(
				"name" -> user.name,
				"admin" -> user.isAdmin
			)

			query.executeInsert()(conn).getOrElse(throw new SQLException("Failed to insert user"))
	}
}
