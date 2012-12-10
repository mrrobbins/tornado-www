
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current
import java.sql.SQLException
import java.sql._

/** A template for inserting a Collection into the database
	* @param name The name for the created collection
  * @param creator The id of the user creating the collection
  */
case class CollectionTemplate(
	name: String,
	creator: Long
)

/** Collection data type
  * @param id The collection id
	* @param name The name of the collection
	* @param time The creation time (millis since the epoch)
	* @param images The ids of the images in the collection
	* @param creator The id of the user that created the collection
  */
case class Collection(
	id: Long,
	name: String,
	time: Long,
	images: Seq[Long],
	creator: Long
)

object Collection {

	def insert(
		template: CollectionTemplate
	)(implicit
		trans: Connection = null
	): Long = ensuringTransaction { implicit trans =>
		import template._
		val newCollection = SQL(
			"""
				INSERT INTO collection (time_created, name, creator) 
				VALUES (UNIX_TIMESTAMP(), {name}, {creator})
			"""
		).on(
			"name" -> name,
			"creator" -> creator
		)

			val priKey = newCollection.executeInsert().getOrElse(
				throw new SQLException("Failed to create collection")
			)

			priKey
	}

	def deleteById(id: Long)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val collection = Collection.withId(id)
		for (imageId <- collection.images) Image.delete(imageId)
		val deleteQuery = SQL(
			"""
				DELETE FROM collection WHERE id={id}
			"""
		).on(
			"id" -> id
		)
		if (1 != deleteQuery.executeUpdate()) throw new SQLException("Not single row")
	}

	def deleteByName(name: String)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val collection = Collection.withName(name)
		for (imageId <- collection.images) Image.delete(imageId)
		val deleteQuery = SQL(
			"""
				DELETE FROM collection WHERE id={id}
			"""
		).on(
			"id" -> collection.id
		)
		if (1 != deleteQuery.executeUpdate()) throw new SQLException("Not single row")
	}

	def withName(name: String)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val collectionQuery = SQL(
			"""
				SELECT * FROM collection WHERE name={name}
			"""
		).on(
			"name" -> name
		)

		val result = collectionQuery().map { row =>
			val id = row[Long]("id")
			val imagesQuery = SQL(
				"""
					SELECT * FROM collection_image WHERE collection_id={id}
				"""
			).on(
				"id" -> id
			)

			val images = imagesQuery().map { row =>
				row[Long]("image_id")
			} toList

			Collection(
				id=row[Long]("id"),
				name=row[String]("name"),
				time=row[Long]("time_created"),
				images=images,
				creator=row[Long]("creator")
			)
		} toList

		result match {
			case List(singleCollection) => singleCollection
			case _ => throw new SQLException("Did not find single collection")
		}
	}

	def withId(id: Long)(implicit trans: Connection = null) = ensuringTransaction { implicit trans =>
		val collectionQuery = SQL(
			"""
				SELECT * FROM collection WHERE id={id}
			"""
		).on(
			"id" -> id
		)

		val result = collectionQuery().map { row =>
			val imagesQuery = SQL(
				"""
					SELECT * FROM collection_image WHERE collection_id={id}
				"""
			).on(
				"id" -> id
			)

			val images = imagesQuery().map { row =>
				row[Long]("image_id")
			} toList

			Collection(
				id=row[Long]("id"),
				name=row[String]("name"),
				time=row[Long]("time_created"),
				images=images,
				creator=row[Long]("creator")
			)
		} toList

		result match {
			case List(singleCollection) => singleCollection
			case _ => throw new SQLException("Did not find single collection")
		}

	}

	def all(implicit conn: Connection = null): List[Collection] = ensuringConnection { implicit conn =>
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
				id=id,
				name=row[String]("name"),
				time=row[Long]("time_created"),
				creator=row[Long]("creator"),
				images=imageList
			)
		} toList

	}

}

