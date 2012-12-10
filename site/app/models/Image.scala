
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
	timeUploaded: Long,
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

	def apply(id: Long)(implicit conn: Connection = null): Image = ensuringConnection { implicit conn =>
		val imageQuery = SQL(		
			"""
				SELECT * FROM (image 
				LEFT JOIN pending_image ON image.id = pending_image.image_id)
				LEFT JOIN collection_image ON image.id = collection_image.image_id 
				WHERE image.id = {imageId};
			"""
		).on("imageId" -> id)
		
		imageQuery() match {
			case Seq(row) =>

				val collectionId = row[Option[Long]]("collection_id")
				// if image is not in collection table it must be in pending table
				if (collectionId.isEmpty && row[Option[Long]]("pending_image.image_id").isEmpty) {
					throw new SQLException("Orphan image - not in a collection or pending")
				}

				// Get notes or throw an exception:
				// First check if collection_image.notes is NULL
				// If not, take that. If so, check pending_image.notes.
				// If both are NULL, we throw an exception
				val notes = row[Option[String]](
					"collection_image.notes"
				) orElse row[Option[String]](
					"pending_image.notes"
				) getOrElse (
					throw new SQLException("Image has no notes")
				)

				val indicator = row[Option[Int]](
					"collection_image.damage_indicator"
				) orElse row[Option[Int]](
					"pending_image.damage_indicator"
				) getOrElse (
					throw new SQLException("Image has no damage indicator")
				)

				val degree = row[Option[Int]](
					"collection_image.degree_of_damage"
				) orElse row[Option[Int]](
					"pending_image.degree_of_damage"
				) getOrElse (
					throw new SQLException("Image has no degree of damage")
				)

				Image(
					id=row[Long]("id"),
					path=row[String]("picture_path"),
					time=row[Option[Long]]("time_captured"),
					timeUploaded=row[Long]("time_uploaded"),
					lat=row[Option[Double]]("latitude"),
					long=row[Option[Double]]("longitude"),
					user=row[Long]("image.user_id"),
					notes=notes,
					indicator=indicator,
					degree=degree,
					collectionId=collectionId,
					pending=collectionId.isEmpty
				)
			case _ => throw new SQLException("Id does not refer to a single image")
		}
	}

	def all(userId: Long = User.NoUser.id)(implicit conn: Connection = null): List[Image] = ensuringConnection { implicit conn =>
		val pendingSelection = """
			SELECT * FROM image JOIN pending_image ON image.id = pending_image.image_id 
		"""

		val collectionSelection = """
			SELECT * FROM image JOIN collection_image ON image.id = collection_image.image_id
		"""

		val filters = Seq(
			if (userId != User.NoUser.id) Some("image.user_id = {userId}")
			else None
		).flatten

		val filterString = if (!filters.isEmpty) " WHERE " + filters.mkString(" AND ") else ""

		val pendingQuery = SQL(
			pendingSelection + filterString
		).on("userId" -> userId)

		val collectionQuery = SQL(
			collectionSelection + filterString
		).on("userId" -> userId)

		val pendingRows = pendingQuery().map { row =>
			Image(
				id=row[Long]("id"),
				path=row[String]("picture_path"),
				time=row[Option[Long]]("time_captured"),
				timeUploaded=row[Long]("time_uploaded"),
				lat=row[Option[Double]]("latitude"),
				long=row[Option[Double]]("longitude"),
				user=row[Long]("user_id"),
				notes=row[String]("notes"),
				indicator=row[Int]("damage_indicator"),
				degree=row[Int]("degree_of_damage"),
				collectionId=None,
				pending=true
			)
		} toList

		val collectionRows = collectionQuery().map { row =>
			Image(
				id=row[Long]("id"),
				path=row[String]("picture_path"),
				time=row[Option[Long]]("time_captured"),
				timeUploaded=row[Long]("time_uploaded"),
				lat=row[Option[Double]]("latitude"),
				long=row[Option[Double]]("longitude"),
				user=row[Long]("user_id"),
				notes=row[String]("notes"),
				indicator=row[Int]("damage_indicator"),
				degree=row[Int]("degree_of_damage"),
				collectionId=Some(row[Long]("collection_id")),
				pending=false
			)
		} toList

		pendingRows ++ collectionRows
	}

	def update(image: Image)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val imageUpdate = SQL(
			"""
				UPDATE image 
				SET latitude = {lat}, longitude = {long} 
				WHERE id = {id};
			"""
		).on(
			"lat" -> image.lat,
			"long" -> image.long,
			"id" -> image.id
		)
	
		if(imageUpdate.executeUpdate() != 1) throw new SQLException("Failed to update image data")

		val tableName = if (image.pending) "pending_image" else "collection_image"

		val secondaryImageUpdate = SQL(
			"""
				UPDATE {table}
				SET notes = {notes}, damage_indicator = {indicator}, degree_of_damage = {degree}
				WHERE image_id = {id};
			""".replace("{table}", tableName)
		).on(
			"notes" -> image.notes,
			"indicator" -> image.indicator,
			"degree" -> image.degree,
			"id" -> image.id
		)

		if(secondaryImageUpdate.executeUpdate() != 1) throw new SQLException("Failed to update collection image data")
	}

	def delete(id: Long)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>

		val image = Image(id)
		
		if (image.pending){
			val pendingImageDelete = SQL(
				"""
					DELETE FROM pending_image WHERE image_id = {id};
				"""
			).on(
				"id" -> id
			)
			if (1 != pendingImageDelete.executeUpdate()) throw new SQLException("Failed to delete image")
		} else {
			val collectionImageDelete = SQL(
				"""
					DELETE FROM collection_image WHERE image_id = {id};
				"""
			).on(
				"id" -> id
			)
			
			if (1 != collectionImageDelete.executeUpdate()) throw new SQLException("Failed to delete image")
		}

		val imageDelete = SQL(
			"""
				DELETE FROM image WHERE id = {id};
			"""
		).on(
				"id" -> id
		)
		if (1 != imageDelete.executeUpdate()) throw new SQLException("Failed to delete image")
	}

	def insert(template: ImageTemplate)(implicit trans: Connection = null): Long = ensuringTransaction { implicit trans =>
		val imageInsertion = SQL(
			"""
				INSERT INTO image (picture_path, time_captured, time_uploaded, latitude, longitude, user_id)
				VALUES ({path}, {time}, UNIX_TIMESTAMP(), {lat}, {long}, {user});
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

	def addToCollection(
		imageId: Long,
		collectionId: Long
	)(implicit
		trans: Connection = null
	) = ensuringTransaction { implicit trans =>

		val image = Image(imageId)

		if (image.pending) {

			val deleteFromPending = SQL(
				"""
					DELETE FROM pending_image WHERE image_id = {imageId} 
				"""
			).on(
				"imageId" -> imageId
			)

			if (1 != deleteFromPending.executeUpdate()) {
				throw new SQLException("Failed to delete pending image")
			}

			val addToCollection = SQL(
				"""
					INSERT INTO collection_image 
						(collection_id, image_id, notes, damage_indicator, degree_of_damage) 
						VALUES ({collectionId}, {imageId}, {notes}, {di}, {dod})
				""").on(
				"collectionId" -> collectionId,
				"imageId" -> imageId,
				"notes" -> image.notes,
				"di" -> image.indicator,
				"dod" -> image.degree
			)

			if (1 != addToCollection.executeUpdate()) {
				throw new SQLException("Failed to add image to collection")
			}
		} else {
			throw new SQLException("Image is not pending")
		}
	}

	def moveToCollection(
		imageId: Long,
		collectionId: Long
	)(implicit
		trans: Connection = null
	) = ensuringTransaction { implicit trans => 
		val moveQuery = SQL(
			"""
				UPDATE collection_image SET collection_id={collection_id}
				WHERE image_id={image_id}
			"""
		)

		if (moveQuery.executeUpdate() != 1)
			throw new SQLException("Image not currently in single collection")
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
				id=row[Long]("image_id"),
				path=row[String]("picture_path"),
				time=row[Option[Long]]("time_captured"),
				timeUploaded=row[Long]("time_uploaded"),
				lat=row[Option[Double]]("latitude"),
				long=row[Option[Double]]("longitude"),
				user=row[Long]("user_id"),
				notes=row[String]("notes"),
				indicator=row[Int]("damage_indicator"),
				degree=row[Int]("degree_of_damage"),
				collectionId=None,
				pending=true
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

