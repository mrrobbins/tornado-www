
package controllers

import data.format.Formats._
import data.Forms._
import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.MimeTypes
import models._
import Json.toJson
import java.io._
import jp.t2v.lab.play20.auth._

object Api extends Controller with Auth with AuthConfigImpl {

	val imgHandler: ImageHandler = new LocalImageHandler

	// default mapping (extension -> MimeType) is M:1
	// Reverse mapping, filter to shortest ext for a given MimeType
	val extensions = {
		val typeList = MimeTypes.types.toList
		val swapped = typeList.map(_.swap)
		swapped.sortBy(_._2.length).reverse
	}.toMap

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

	def createCollection = Action(parse.urlFormEncoded) { request =>
		Ok(request.body.toString)	
		// get the id for all images selected
		val checkedRows = request.body.filter(row => row._2.contains("on")).keySet
		val selectedImages = checkedRows.toSeq.map(_.toLong)
		
		val primaryImage = Image.pending(selectedImages.head).get
		val newCollection = CollectionTemplate(
			"[address]",
			"[location_description]",
			primaryImage.notes,
			primaryImage.indicator,
			primaryImage.degree,
			primaryImage.id,
			selectedImages.tail
		)

		Collection.insert(newCollection)

		Redirect("/photoqueue")

		/*val form = collectionForm.bindFromRequest()(request).get*/
		/*val inserted = {*/
		/*	import form._*/
		/*	CollectionTemplate(*/
		/*		_1,*/
		/*		_2,*/
		/*		_3,*/
		/*		_4,*/
		/*		_5,*/
		/*		0l,*/
		/*		Nil*/
		/*	)*/
		/*}*/
		/*Collection.insert(inserted)	*/
		/*Ok("success")*/
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

		try {
			val metadata = ImageMetadataReader.readMetadata(file)
			// obtain the Exif directory
			val gpsDirectory: Option[GpsDirectory] = Option(metadata.getDirectory(classOf[GpsDirectory]))

			val gpsInfo = gpsDirectory.flatMap(d => Option(d.getGeoLocation()))
			val latitude = gpsInfo.flatMap(g => Option(g.getLatitude()))
			val longitude = gpsInfo.flatMap(g => Option(g.getLongitude()))
			
			val dateDirectory: Option[ExifSubIFDDirectory] = Option(metadata.getDirectory(classOf[ExifSubIFDDirectory]))
			val date: Option[Date] = dateDirectory.flatMap(d => Option(d.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)))
			val unixTimestamp = date.flatMap(d => Option(d.getTime()));

			assert(latitude != null)
			assert(longitude != null)
			assert(unixTimestamp != null)

			ImageMetadata(latitude, longitude, unixTimestamp)
		} catch {
			case ipe: ImageProcessingException => ImageMetadata(None, None, None)
		}
	}

	def imageUpload = authorizedAction(parse.multipartFormData, NormalUser) { implicit user => implicit request =>
		println(user.toString)
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

