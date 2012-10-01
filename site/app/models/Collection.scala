
package models

import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException

case class CollectionTemplate(
	address: String,
	locationDescription: String,
	notes: String,
  damageIndicator: Int,
  degreeOfDamage: Int,
	primaryImageId: Long,
	secondaryImages: Seq[Long]
)

case class Collection(
	id: Long,
	time: Long,
	gpsAddress: String,
	address: String,
	locationDescription: String,
	notes: String,
	damageIndicator: Int,
	degreeOfDamage: Int,
	primaryImage: Long,
	secondaryImages: Seq[Long]
)

object Collection {

	def insert(c: CollectionTemplate): Option[Long] = DB.withTransaction { conn: java.sql.Connection =>
		val newCollection = SQL(
			"""
				INSERT INTO collection (time_created, gps_address, address, location_description, notes, primary_image_id) 
				VALUES (UNIX_TIMESTAMP(), {gpsAddr}, {addr}, {desc}, {notes}, {priImg})
			"""
		).on(
			"gpsAddr" -> "",
			"addr" -> c.address, 
			"desc" -> c.locationDescription,
			"notes" -> c.notes,
			"priImg" -> c.primaryImageId
		)

		try {
			val priKey = newCollection.executeInsert()(conn).getOrElse(throw new SQLException("Failed to create collection"))

			for (id <- c.secondaryImages :+ c.primaryImageId) {
				val pendingImage = PendingImage.withId(id)
				PendingImage.remove(id)
				Image.setCollection(id, priKey)
			}

			conn.commit()
			Some(priKey)
		} catch {
			case e: java.sql.SQLException => 
				e.printStackTrace()
				conn.rollback()
				None
			case e: Exception =>
				e.printStackTrace()
				conn.rollback()
				None
		}
	}
}

