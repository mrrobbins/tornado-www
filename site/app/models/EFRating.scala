
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException
import java.sql._

object EFRating {
	def apply(
		speed: Int
	)(implicit
		conn: Connection = null
	) = ensuringConnection("domain") { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM tornado_wind_speed
				WHERE minimum_speed <= {speed}
				ORDER BY minimum_speed DESC
			"""
		).on(
			"speed" -> speed
		)

		query().headOption.map { row =>
			row[Int]("rating")
		}
	}

	def all(implicit
		conn: Connection = null
	) = ensuringConnection("domain") { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM tornado_wind_speed
			"""
		)

		query().map { row =>
			(
				row[Int]("rating"),
				row[Int]("minimum_speed")
			)
		} toList
	}

}
