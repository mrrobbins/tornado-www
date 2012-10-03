
package models

import play.api.Play.current
import java.sql._
import anorm._
import play.api.db.DB

case class Image(
	id: Long,
	path: String,
	time: Long,
	lat: Double,
	long: Double,
	user: Int,
	notes: String,
	indicator: Int,
	degree: Int,
	collectionId: Option[Long],
	pending: Boolean
) {

	def addToCollection(collectionId: Long) { Image.addToCollection(id, collectionId) }
}

object Image {
	def addToCollection(imageId: Long, collectionId: Long, fromCollectionId: Option[Long] = None) = 
		DB.withTransaction { conn: java.sql.Connection => 

		val maybePending = Image.pending(conn)(imageId)

		if (maybePending.isDefined) {
			val pending = maybePending.get

			val deletePending = SQL(
				"""
					DELETE FROM pending_image WHERE image_id = {imageId} 
				"""
			).on(
				"imageId" -> imageId
			)

			if (1 != deletePending.executeUpdate()(conn)) {
				throw new SQLException("Failed to delete pending image")
			}

			val addImage = SQL(
				"""
					INSERT INTO collection_image 
						(collection_id, image_id, notes, damage_indicator, degree_of_damage) 
						VALUES ({collectionId}, {imageId}, {notes}, {di}, {dod})
				""").on(
				"collectionId" -> collectionId,
				"imageId" -> imageId,
				"notes" -> pending.notes,
				"di" -> pending.indicator,
				"dod" -> pending.degree
			)

			if (1 != addImage.executeUpdate()(conn)) {
				throw new SQLException("Failed to add image to collection")
			}
		} else {
			val collections = Image.collections(conn)(imageId)
				
			val (notes, di, dod) = if (fromCollectionId.isDefined) {
				val damageProperties = SQL(
					"""
						SELECT notes, damage_indicator, degree_of_damage FROM collection_image WHERE collection_id = {collectionId} AND image_id = {imageId}
					"""
				).on(
					"collectionId" -> fromCollectionId.get,
					"image_id" -> imageId
				)
				val head = damageProperties()(conn).headOption
				head.map { row => 
					import row.get
					(
						get[String]("notes"),
						get[Int]("damage_indicator"), 
						get[Int]("degree_of_damage")
					)
				}.getOrElse(("", -1, -1))
			} else {
				("", -1, -1)	
			}

			val addImage = SQL(
				"""
					INSERT INTO collection_image 
						(collection_id, image_id, notes, damage_indicator, degree_of_damage) 
						VALUES ({collectionId}, {imageId}, {notes}, {di}, {dod})
				""").on(
				"collectionId" -> collectionId,
				"imageId" -> imageId,
				"notes" -> notes,
				"di" -> di,
				"dod" -> dod 
			)
		}

		conn.commit()
	}

	def pending(conn: Connection)(imageId: Long): Option[Image] = {
		val maybePending = SQL(
			"""
				SELECT * FROM pending_image JOIN image ON pending_image.image_id = image.id 
				WHERE pending_image.image_id = {imageId}
			"""
		).on(
			"imageId" -> imageId
		)
		
		val images: Stream[SqlRow] = maybePending()(conn)		
		val head = images.headOption
		head.map { row => 
			import row.get
			Image(
				get[Long]("image_id").get,
				get[String]("picture_path").get,
				get[Long]("time_captured").get,
				get[Double]("latitude").get,
				get[Double]("longitude").get,
				get[Int]("user_id").get,
				get[String]("notes").get,
				get[Int]("damage_indicator").get,
				get[Int]("degree_of_damage").get,
				None,
				true
			)
		}
	}

	def collections(conn: Connection)(imageId: Long): List[Long] = {
		val selectCollections = SQL(
			"""
				SELECT UNIQUE(collection_id) FROM collection_image WHERE image_id = {imageId}
			"""
		).on(
			"imageId" -> imageId
		) 

		val collections: Stream[SqlRow] = selectCollections()(conn) 
		collections.map { row => 
			row.get[Long]("collection_id").get
		}.toList
	}
}

