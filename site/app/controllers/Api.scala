
package controllers

import data.format.Formats._
import data.Forms._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.MimeTypes
import models._
import Json.toJson
import java.io._

object Api extends Controller {

	val imgHandler: ImageHandler = new LocalImageHandler

	// Map (extension -> MimeType) is M:1
	// Reverse mapping, filter to shortest ext for a given MimeType
	val extensions = {
		val typeList = MimeTypes.types.toList
		val swapped = typeList.map(_.swap)
		swapped.sortBy(_._2.length).reverse
	} toMap

	def addToCollection(imageId: Long, collectionId: Long, fromCollectionId: Option[Long]) = {
		Action { request => 
		Ok(Image.addToCollection(imageId, collectionId).toString)
		}
	}

	val collectionForm = Form(
		tuple(
			"address" -> text,
			"location_description" -> text,
			"notes" -> text,
      "indicator" -> number,
      "degree" -> number
		)
	)

	def createCollection = Action { request =>
		val form = collectionForm.bindFromRequest()(request).get
		val inserted = {
			import form._
			CollectionTemplate(
				_1,
				_2,
				_3,
				_4,
				_5,
				0l,
				Nil
			)
		}
		Collection.insert(inserted)	
		Ok("success")
	}

	def allMarkers = Action {

		val allMarkers = Marker.all.map(marker =>
			Map(
				"id" -> toJson(marker.id),
				"name" -> toJson(marker.description), 
				"lat" -> toJson(marker.latitude), 
				"long" -> toJson(marker.longitude),
				"type" -> toJson(marker.markerType)
			)
		)

		val allMarkersJson = toJson(Map("markers" -> toJson(allMarkers)))

		Ok(allMarkersJson)
	}

	def balloon(imageId: Long) = Action {

		val image = Image.all.filter(image => image.id == imageId).head
		val content = views.html.balloon(image).toString
	
		val contentJson = toJson(Map("content" -> toJson(content)))
		
		Ok(contentJson)	
	}

	class GpsMetadataException(msg: String) extends RuntimeException(msg)

	def imgMetadata(file: File): Map[String, Double] = {
		import com.drew.imaging._
		import com.drew.metadata.exif._
		import com.drew.lang._

		val metadata = ImageMetadataReader.readMetadata(file)
		// obtain the Exif directory
		val directory: GpsDirectory = metadata.getDirectory(classOf[GpsDirectory])

		if (directory == null) throw new GpsMetadataException("No GPS info")

		val gpsInfo: GeoLocation = directory.getGeoLocation()	

		if (gpsInfo == null) throw new GpsMetadataException("No GPS info")

		Map("latitude" -> gpsInfo.getLatitude, "longitude" -> gpsInfo.getLongitude)
	}

	def imageUpload = Action(parse.multipartFormData) { request =>
		// remove missing or non-image mime types
		val (imageFiles, errorFiles) = request.body.files.partition(_.contentType.filter(_.startsWith("image/")).isDefined)
		val fileStorageErrors = imageFiles.flatMap { file =>
			val name = file.filename
			val size = file.ref.file.length
			val metadata = try {
				Some(imgMetadata(file.ref.file))
			} catch {
				case gme: GpsMetadataException => None
			}
			if (metadata.isDefined) {
				println("Gps data: lat:%f, lng:%f".format(metadata.get("latitude"), metadata.get("longitude")))
			} else {
				println("No Gps data")
			}
			try {
				val path = imgHandler.store(file)
				None
			} catch {
				case ioe: IOException => 
					Some(Json.toJson(Map(
						"name" -> toJson(file.filename),
						"error" -> toJson("Failed to store image")
					)))
			}
		}

		/*val imageFileResponses = fileProperties.map { case (name, path, size) => */
		/*	Json.toJson(Map(*/
		/*		"name" -> toJson(name),*/
		/*		"size" -> toJson(size),*/
		/*		"url" -> toJson(imgHandler.lookup(path)),*/
		/*		"thumbnail_url" -> toJson(imgHandler.lookup(path))*/
		/*	))*/
		/*}*/

		val fileTypeErrors = errorFiles.map { file =>
			Json.toJson(Map(
				"name" -> toJson(file.filename),
				"error" -> toJson("Not an image file!")
			))
		}

		val result = Json.toJson(fileStorageErrors++fileTypeErrors)

		Ok(result)
	}
}

