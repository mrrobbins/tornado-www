
package models

import ModelHelpers._
import play.api.Play.current
import java.sql._
import anorm._
import play.api.db.DB

case class Image(
	id: Long,
	path: String,
	time: Option[Long],
	lat: Option[Double],
	long: Option[Double],
	user: Long,
	notes: String,
	indicator: Int,
	degree: Int,
	collectionId: Option[Long],
	pending: Boolean
) {

	def addToCollection(collectionId: Long) { Image.addToCollection(id, collectionId) }
}

case class ImageTemplate(
	path: String,
	lat: Option[Double],
	long: Option[Double],
	userId: Long,
	time: Option[Long],
	notes: String,
	indicator: Int,
	degree: Int
)

object Image {

	def all(implicit conn: Connection = null): List[Image] = ensuringConnection { implicit conn =>
		val pendingQuery = SQL(
			"""
				SELECT * FROM image JOIN pending_image ON image.id = pending_image.image_id;
			"""
		)

		val collectionQuery = SQL(
			"""
				SELECT * FROM image JOIN collection_image ON image.id = collection_image.image_id;
			"""
		)

		val pendingRows = pendingQuery().map { row =>
			Image(
				row[Long]("id"),
				row[String]("picture_path"),
				row[Option[Long]]("time_captured"),
				row[Option[Double]]("latitude"),
				row[Option[Double]]("longitude"),
				row[Long]("user_id"),
				row[String]("notes"),
				row[Int]("damage_indicator"),
				row[Int]("degree_of_damage"),
				None,
				true
			)
		} toList

		val collectionRows = collectionQuery().map { row =>
			Image(
				row[Long]("id"),
				row[String]("picture_path"),
				row[Option[Long]]("time_captured"),
				row[Option[Double]]("latitude"),
				row[Option[Double]]("longitude"),
				row[Long]("user_id"),
				row[String]("notes"),
				row[Int]("damage_indicator"),
				row[Int]("degree_of_damage"),
				Some(row[Long]("collection_id")),
				false
			)
		} toList

		pendingRows ++ collectionRows

	}

	def insert(template: ImageTemplate)(implicit trans: Connection = null): Long = ensuringTransaction { implicit trans =>
			val imageInsertion = SQL(
				"""
					INSERT INTO image (picture_path, time_captured, latitude, longitude, user_id)
					VALUES ({path}, {time}, {lat}, {long}, {user});
				"""
			).on(
					"path" -> template.path,
					"time" -> template.time,
					"lat" -> template.lat,
					"long" -> template.long,
					"user" -> template.userId
			)

			val image = imageInsertion.executeInsert().getOrElse(throw new SQLException("Failed to create image"))
			val pendingInsert = SQL(
				"""
					INSERT INTO pending_image (image_id, notes, damage_indicator, degree_of_damage, user_id)
					VALUES ({image}, {notes}, {indicator}, {degree}, {user});
				"""
			).on(
				"image" -> image,
				"notes" -> template.notes,
				"indicator" -> template.indicator,
				"degree" -> template.degree,
				"user" -> template.userId
			)

			if (pendingInsert.executeUpdate() != 1) throw new SQLException("Failed to add to pending")

			image
	}

	def addToCollection(imageId: Long, collectionId: Long, fromCollectionId: Option[Long] = None)(implicit trans: Connection = null): Unit = ensuringTransaction { implicit trans =>

		val maybePending = Image.pending(imageId)

		if (maybePending.isDefined) {
			val pending = maybePending.get

			val deletePending = SQL(
				"""
					DELETE FROM pending_image WHERE image_id = {imageId} 
				"""
			).on(
				"imageId" -> imageId
			)

			if (1 != deletePending.executeUpdate()) {
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

			if (1 != addImage.executeUpdate()) {
				throw new SQLException("Failed to add image to collection")
			}
		} else {
			val collections = Image.collections(imageId)
				
			val (notes, di, dod) = if (fromCollectionId.isDefined) {
				val damageProperties = SQL(
					"""
						SELECT notes, damage_indicator, degree_of_damage FROM collection_image WHERE collection_id = {collectionId} AND image_id = {imageId}
					"""
				).on(
					"collectionId" -> fromCollectionId.get,
					"image_id" -> imageId
				)
				val head = damageProperties().headOption
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

	}

	def pending(imageId: Long)(implicit conn: Connection = null): Option[Image] = ensuringConnection { implicit conn =>
		val maybePending = SQL(
			"""
				SELECT * FROM pending_image JOIN image ON pending_image.image_id = image.id 
				WHERE pending_image.image_id = {imageId}
			"""
		).on(
			"imageId" -> imageId
		)
		
		val images: Stream[SqlRow] = maybePending()		
		val head = images.headOption
		head.map { row => 
			import row.get
			Image(
				row[Long]("image_id"),
				row[String]("picture_path"),
				row[Option[Long]]("time_captured"),
				row[Option[Double]]("latitude"),
				row[Option[Double]]("longitude"),
				row[Long]("user_id"),
				row[String]("notes"),
				row[Int]("damage_indicator"),
				row[Int]("degree_of_damage"),
				None,
				true
			)
		}
	}

	def collections(imageId: Long)(implicit conn: Connection = null): List[Long] = ensuringConnection { implicit conn =>
		val selectCollections = SQL(
			"""
				SELECT UNIQUE(collection_id) FROM collection_image WHERE image_id = {imageId}
			"""
		).on(
			"imageId" -> imageId
		) 

		val collections: Stream[SqlRow] = selectCollections() 
		collections.map { row => 
			row.get[Long]("collection_id").get
		}.toList
	}
}

