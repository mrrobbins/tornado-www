
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException
import java.sql._

case class CollectionTemplate(
	address: String,
	locationDescription: String,
	notes: String,
  damageIndicator: Int,
  degreeOfDamage: Int,
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
	secondaryImages: Seq[Long]
)

object Collection {

	def insert(c: CollectionTemplate)(implicit trans: Connection = null): Long = ensuringTransaction { implicit trans =>
		val newCollection = SQL(
			"""
				INSERT INTO collection (time_created, gps_address, address, location_description, notes, damage_indicator, degree_of_damage) 
				VALUES (UNIX_TIMESTAMP(), {gpsAddr}, {addr}, {desc}, {notes}, {indicator}, {degree})
			"""
		).on(
			"gpsAddr" -> "",
			"addr" -> c.address, 
			"desc" -> c.locationDescription,
			"notes" -> c.notes,
			"indicator" -> c.damageIndicator,
			"degree" -> c.degreeOfDamage
		)

			val priKey = newCollection.executeInsert().getOrElse(throw new SQLException("Failed to create collection"))

			for (id <- c.secondaryImages) {
				val pendingImage = Image.pending(id)
				Image.addToCollection(id, priKey)
			}

			priKey
	}

	def all(implicit conn: Connection = null): List[Collection] = ensuringConnection { implicit conn =>
		assert(conn != null)
		val query = SQL(
			"""
				SELECT * FROM collection;
			"""
		)

		query().map { row =>
			val id = row[Long]("id")
			val imagesQuery = SQL(
				"""
					select image_id from collection_image where collection_image.collection_id = {id};
				"""
			).on(
				"id" -> id
			)

			val imageList = imagesQuery().map { row =>
				row[Long]("image_id")
			} toList

			Collection(
				row[Long]("primary_image_id"),
				row[Long]("time_created"),
				row[String]("gps_address"),
				row[String]("address"),
				row[String]("location_description"),
				row[String]("notes"),
				row[Int]("damage_indicator"),
				row[Int]("degree_of_damage"),
				imageList
			)
		} toList

	}
}

