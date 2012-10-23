
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

	// default mapping (extension -> MimeType) is M:1
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

	case class ImageMetadata(
		latitude: Option[Double],
		longitude: Option[Double],
		time: Option[Long]
	)

	def imageMetadata(file: File): ImageMetadata = {
		import com.drew.imaging._
		import com.drew.metadata.exif._
		import com.drew.lang._
		import java.util.Date

		val metadata = ImageMetadataReader.readMetadata(file)
		// obtain the Exif directory
		val gpsDirectory: GpsDirectory = metadata.getDirectory(classOf[GpsDirectory])

		val (latitude, longitude) = if (gpsDirectory != null) {
			val gpsInfo: GeoLocation = gpsDirectory.getGeoLocation()	
			val latitude = Option(gpsInfo).map(_.getLatitude)
			val longitude = Option(gpsInfo).map(_.getLongitude)
			(latitude, longitude)
		} else {
			(None, None)
		}
		
		val dateDirectory: ExifSubIFDDirectory = metadata.getDirectory(classOf[ExifSubIFDDirectory])
		val date: Date = dateDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
		val unixTimestamp = Option(date).map(_.getTime());	

		ImageMetadata(latitude, longitude, unixTimestamp)
	}

	def imageUpload = Action(parse.multipartFormData) { request =>
		// remove missing or non-image mime types
		val (imageFiles, errorFiles) = request.body.files.partition(_.contentType.filter(_.startsWith("image/")).isDefined)
		val fileStorageErrors = imageFiles.flatMap { file =>
			val name = file.filename
			val size = file.ref.file.length
			val metadata: ImageMetadata = imageMetadata(file.ref.file)
			// store with metadata
			println("Metadata: time:%d, lat:%f, lng:%f".format(
				metadata.time.getOrElse(-1l),
				metadata.latitude.getOrElse(Double.NaN),
				metadata.longitude.getOrElse(Double.NaN)
			))
			try {
				val path = imgHandler.store(file)
				val template: ImageTemplate = ImageTemplate(
					path,
					metadata.latitude,
					metadata.longitude,
					User.all.head.id,
					metadata.time,
					"",
					1,
					1
				)
				Image.insert(template)
				None
			} catch {
				case ioe: IOException => 
					Some(Json.toJson(Map(
						"name" -> toJson(file.filename),
						"error" -> toJson("Failed to store image")
					)))
			}
		}

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

