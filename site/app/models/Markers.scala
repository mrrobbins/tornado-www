
package models

import anorm._
import play.api.db.DB
import play.api.Play.current

case class Marker(
	description: String,
	latitude: Double,
	longitude: Double
)

object Marker {
	
	def all() = DB.withConnection { c =>
		val selectAllMarkers = SQL(
			"""
				SELECT * FROM report;
			"""
		)

		val rows = selectAllMarkers()(c)

		rows.map { row =>
			Marker(row[String]("location_description"), row[Double]("latitude"), row[Double]("longitude"))
		}.toList
	}
}
