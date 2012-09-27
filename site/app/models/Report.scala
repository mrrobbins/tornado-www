
package models

import anorm._
import play.api.db.DB
import play.api.Play.current

case class CollectionTemplate(
	gps_address: String,
	address: String,
	locationDescription: String,
	notes: String,
  damageIndicator: Int,
  degreeOfDamage: Int,
	primaryImageId: Int,
	secondaryImages: Seq[Int]
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
	primaryImage: (PendingImageTemplate),
	secondaryImages: Seq[PendingImageTemplate)]
)

object Collection {

	def insert(r: CollectionTemplate): Option[Long] = DB.withTransaction { c =>
		val newCollection = SQL(
			"""
				INSERT INTO collection (time, gps_address, address, location_description, notes, damage_indicator, degree_of_damage, primary_image_id) 
				VALUES (UNIX_TIMESTAMP(), {gpsAddr}, {addr}, {desc}, {notes}, {indicator}, {degree}, {priImg})
			"""
		).on(
			"gpsAddr" -> r.gpsAddress,
			"addr" -> r.address, 
			"desc" -> r.locationDescription,
			"notes" -> r.notes,
      "indicator" -> r.damageIndicator,
      "degree" -> r.degreeOfDamage,
			"priImg" -> r.primaryImageId
		)

		try {
			val priKey = newCollection.executeInsert()(c)

			for (pendingImg <- r.secondaryImage :+ r.primaryImage) {
				val newCollectionImage = Sql(
					"""
						INSERT INTO collection_image (collection_id, image_id, damage_indicator, degree_of_damage, notes) 
						VALUES ({cid}, {iid}, {di}, {dd}, {notes})
					"""
				).on(
					"cid" -> priKey,
					"iid" -> pendingImg.imageId,
					"di" -> pendingImg.damageIndicator,
					"dd" -> pendingImg.damageDegree,
					"notes" -> pendingImg.notes
				)
				newCollectionImage.executeInsert()(c)
			}

			Some(priKey)
		} catch {
			case e: java.sql.SqlException => 
				e.printStackTrace()
				c.rollback()
				None
			case e: Exception =>
				e.printStackTrace()
				c.rollback()
				None
		}
	}
}

