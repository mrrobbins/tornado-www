
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

	def insert(user: User) = DB.withConnection { conn =>
		val query = SQL(
			"""
				INSERT INTO user (id, username, admin)
				VALUES ({id}, {name}, {admin})
			""").on(
				"name" -> user.name,
				"id" -> user.id,
				"admin" -> user.isAdmin
			)

			if (query.executeUpdate()(conn) != 1) {
				throw new SQLException("Failed to create user")
			}
	}
}
