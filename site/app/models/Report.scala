
package models

import anorm._
import play.api.db.DB
import play.api.Play.current

case class Report(
	id: Option[Int],
	latitude: Double,
	longitude: Double,
	address: String,
	locationDescription: String,
	notes: String
)

object Report {
	def insert(r: Report) = DB.withConnection { c =>
		val newReport = SQL(
			"""
				INSERT INTO report VALUES({id}, NULL, NULL, {lat}, {long}, {addr}, {desc}, {notes}, NULL, NULL, NULL)
			"""
		).on(
			"id" -> r.id, 
			"lat" -> r.latitude, 
			"long" -> r.longitude, 
			"addr" -> r.address, 
			"desc" -> r.locationDescription,
			"notes" -> r.notes
		)
		
		newReport.executeUpdate()(c)
	}
}

