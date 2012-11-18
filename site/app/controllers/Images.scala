
package controllers

import play.api.mvc._
import models._
import jp.t2v.lab.play20.auth._
import play.api.data.Forms._
import play.api.data.Form
import java.io._
import play.api.libs.json._
import play.api.libs.json.Json.toJson
import play.api.libs.concurrent.Akka
import play.api.Play.current
import java.util.Date
import java.text.DateFormat
import java.util.Locale
import data.Forms._

object Images extends Controller with Auth with AuthConfigImpl {

	def queuePage =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.photoQueue())	
		}

	def uploadPage =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.upload()) 
		}	

	def edit(imageId: Long) =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			val form = imageToFormData(Image(imageId))
			Ok(views.html.image(editImageForm.fill(form)))
		}	

	def editSubmit =
		authorizedAction(NormalUser) { implicit user => implicit request =>
		editImageForm.bindFromRequest()
		def success(data: EditImageFormData) = Async {
			val image = Image(data.id).copy(
				notes=data.notes, 
				indicator=data.indicator, 
				degree=data.degree,
				lat=data.latitude,
				long=data.longitude
			)
			Akka.future {
				Image.update(image)
				Ok("/photoqueue")
			} recover {
				case _: Exception =>
					val form = editImageForm.fill(data)
					val flashSession = flash + ("message" -> "Failed to update image")
					InternalServerError(views.html.image(form)(flashSession, user))
			}
		}
		def failure(form: Form[EditImageFormData]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			val imageId = form("id").value
			if (imageId.isDefined) {
				val image = Image(imageId.get.toLong)
				println(json.toString)
				BadRequest(views.html.image(form)(flash + ("message" -> error), user))
			} else {
				Redirect("/photoqueue")
			}
		}

		editImageForm.bindFromRequest().fold(failure, success)
	}	

	def imageToFormData(image: Image) = {
		import image._
		val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
		EditImageFormData(
			id,
			path,
			time.map(t => df.format(new Date(t))),
			df.format(new Date(timeUploaded*1000L)),
			lat,
			long,
			user,
			notes,
			indicator,
			degree,
			collectionId,
			pending.toString
		)	
	}

	case class EditImageFormData(
		id: Long,
		path: String,
		time: Option[String],
		uploaded: String,
		latitude: Option[Double],
		longitude: Option[Double],
		user: Long,
		notes: String,
		indicator: Int,
		degree: Int,
		collectionId: Option[Long],
		pending: String
	)

	val editImageForm = Form (
		mapping(
			"id" -> longNumber,
			"path" -> text,
			"time" -> optional(text),
			"uploaded" -> text,
			"lat" -> optional(doubleDecimal),
			"long" -> optional(doubleDecimal),
			"user" -> longNumber,
			"notes" -> text,
			"indicator" -> number,
			"degree" -> number,
			"collectionId" -> optional(longNumber),
			"pending" -> text
		)(EditImageFormData.apply)(EditImageFormData.unapply)
	)

	object Api {

		def addToCollection(
			imageId: Long,
			collectionId: Long,
			fromCollectionId: Option[Long]
		) = authorizedAction(NormalUser) { implicit user => implicit request => 
			Async { Akka.future {
				Ok(Image.addToCollection(imageId, collectionId).toString)
			} }
		}

		def createCollection = Action(parse.urlFormEncoded) { request =>
			// get the id for all images selected
			val checkedRows =
				request.body.filter(row => row._2.contains("on")).keySet
			val selectedImages = checkedRows.toSeq.map(_.toLong)
			
			Async { Akka.future {
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
			} }

		}

		def uploadImage =
			authorizedAction(
				parse.multipartFormData,
				NormalUser
			) { implicit user => implicit request =>
				// remove missing or non-image mime types
				val (imageFiles, errorFiles) = request.body.files.partition(
						_.contentType.filter(_.startsWith("image/")).isDefined
				)
				val futureFileStorageErrors = Akka.future { imageFiles.flatMap { file =>
					val name = file.filename
					val size = file.ref.file.length
					val metadata: ImageMetadata = imageMetadata(file.ref.file)
					// store with metadata
					try {
						val tmpFile = File.createTempFile("img_thumb", ".jpg")
						ImageThumbnailer.createThumbnail(file.ref.file, tmpFile)
						val path = StorageBackend().store("images", file.ref.file, file.contentType)
						StorageBackend().store("thumbnails", tmpFile, Some("image/jpg"), Some(path))
						val template: ImageTemplate = ImageTemplate(
							path,
							metadata.latitude,
							metadata.longitude,
							user.id,
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
				} }

				val fileTypeErrors = errorFiles.map { file =>
					Json.toJson(Map(
						"name" -> toJson(file.filename),
						"error" -> toJson("Not an image file!")
					))
				}

				Async { futureFileStorageErrors.map { fileStorageErrors =>
					val errors = fileStorageErrors ++ fileTypeErrors
					val result = Json.toJson(errors)
					if (errors isEmpty) Ok(result)
					else BadRequest(result)
				} }

			}

	}


	/** Data type representing the relevant metadata from an image file
	  * @param latitude the latitude from the image (if present) in degrees
	  * @param longitude the longitude from the image (if present) in degrees
	  * @param time the capture time from the image (if present) in milliseconds since the Unix epoch
	  */
	case class ImageMetadata(
		latitude: Option[Double],
		longitude: Option[Double],
		time: Option[Long]
	)

	/** Extract image metadata from a jpeg file
	  * @param file the file to extract the data from
	  * @return the extracted data
	  * @todo handle more image types
	  */
	def imageMetadata(file: File): ImageMetadata = {
		import com.drew.imaging._
		import com.drew.metadata.exif._
		import com.drew.lang._
		import java.util.Date

		try {
			val metadata = ImageMetadataReader.readMetadata(file)

			// obtain the Exif directory
			val gpsDirectory: Option[GpsDirectory] =
				Option(metadata.getDirectory(classOf[GpsDirectory]))

			val gpsInfo = gpsDirectory.flatMap(d => Option(d.getGeoLocation()))
			val latitude = gpsInfo.flatMap(g => Option(g.getLatitude()))
			val longitude = gpsInfo.flatMap(g => Option(g.getLongitude()))
			
			val dateDirectory: Option[ExifSubIFDDirectory] =
				Option(metadata.getDirectory(classOf[ExifSubIFDDirectory]))

			val date: Option[Date] = dateDirectory.flatMap { d =>
				Option(d.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
			}

			// Actually in milliseconds, as is the Java way
			val unixTimestamp = date.flatMap(d => Option(d.getTime()))

			assert(latitude != null)
			assert(longitude != null)
			assert(unixTimestamp != null)

			ImageMetadata(latitude, longitude, unixTimestamp)
		} catch {
			case ipe: ImageProcessingException => ImageMetadata(None, None, None)
		}
	}
}
