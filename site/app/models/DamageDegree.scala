
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException
import java.sql._

case class DamageDegree(
	id: Long,
	indicatorAbbreviation: String,
	description: String,
	minWindSpeed: Int,
	expectedWindSpeed: Int,
	maxWindSpeed: Int
) {
	lazy val indicator = DamageIndicator.withAbbrev(indicatorAbbreviation)
}

object DamageDegree {

	def all(implicit conn: Connection = null) = ensuringConnection("domain") { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM tornado_degree_of_damage
			"""
		)

		val degrees = query().map { row =>
			DamageDegree(
				row[Long]("id"),
				row[String]("indicator_abbreviation"),
				row[String]("description"),
				row[Int]("lowest_windspeed"),
				row[Int]("expected_windspeed"),
				row[Int]("highest_windspeed")
			)
		} toList

		degrees

	}

}
