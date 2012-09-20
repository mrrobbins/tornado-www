
package models

import anorm._
import play.api.db.DB
import play.api.Play.current

case class ReportTemplate(
	latitude: Double,
	longitude: Double,
	address: String,
	locationDescription: String,
	notes: String,
  damageIndicator: Int,
  degreeOfDamage: Int
)

case class Report(
	id: Long,
	picturePath: Option[String],
	time: Long,
	latitude: Double,
	longitude: Double,
	address: String,
	locationDescription: String,
	notes: String,
	damageIndicator: Int,
	degreeOfDamage: Int
)


object Report {

	def insert(r: ReportTemplate) = DB.withConnection { c =>
		val newReport = SQL(
			"""
				INSERT INTO report (time, latitude, longitude, address, location_description, notes, damage_indicator, degree_of_damage, picture_path) VALUES
        (UNIX_TIMESTAMP(), {lat}, {long}, {addr}, {desc}, {notes}, {indicator}, {degree}, NULL)
			"""
		).on(
			"lat" -> r.latitude, 
			"long" -> r.longitude, 
			"addr" -> r.address, 
			"desc" -> r.locationDescription,
			"notes" -> r.notes,
      "indicator" -> r.damageIndicator,
      "degree" -> r.degreeOfDamage
		)
		
		newReport.executeUpdate()(c)
	}
}

