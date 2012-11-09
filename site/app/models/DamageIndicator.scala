
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException
import java.sql._

case class DamageIndicator(
	id: Long,
	description: String,
	abbreviation: String
) {
	lazy val degrees = DamageDegree.all.filter(_.indicatorAbbreviation == abbreviation)
}

object DamageIndicator {

	def all(implicit conn: Connection = null) = ensuringConnection("domain") { implicit conn =>
		val query = SQL(
			"""
				SELECT * FROM tornado_damage_indicator
			"""
		)

		val indicators = query().map { row =>
			DamageIndicator(
				row[Long]("id"),
				row[String]("abbreviation"),
				row[String]("description")
			)
		} toList

		indicators

	}

	def withAbbrev(abbrev: String)(implicit conn: Connection = null) = ensuringConnection("domain") { implicit conn =>
		all.find(_.abbreviation == abbrev).getOrElse(throw new NoSuchElementException("No indicator with given abbreviation"))
	}

}


