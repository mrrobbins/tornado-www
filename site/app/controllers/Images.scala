
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
import java.sql.SQLException
import models.ModelHelpers.ensuringTransaction

object Images extends Controller with Auth with AuthConfigImpl {

	def queuePage = authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.photoQueue()).withSession(
				session + ("lastEditor" -> "/photoqueue")
			)
	}

	def uploadPage = authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.upload()) 
	}	

	def edit(imageId: Long) =
		optionalUserAction { implicit maybeUser => implicit request =>
			val image = Image(imageId)
			if (image.pending && !maybeUser.forall(_.id == image.user)) Forbidden("Not your image")
			else {
				val form = imageToFormData(image)
				Ok(
					views.html.image(
						imageId,
						editImageForm.fill(form)
					)
				)
			}
		}	

	def editSubmit(imageId: Long) = authorizedAction(NormalUser) { implicit user => implicit request => 
		editImageForm.bindFromRequest()
		def success(data: EditImageFormData) = Async {
			Akka.future {
				val orig = Image(imageId)
				if (orig.user != user.id && !user.isAdmin) Forbidden("Not your image")
				else {
					val image = orig.copy(
						notes=data.notes, 
						indicator=data.indicator, 
						degree=data.degree,
						lat=data.latitude,
						long=data.longitude
					)
					Image.update(image)
					println("here")
					session.get("lastEditor").map { lastEditor =>
						println(lastEditor)
						Redirect(lastEditor)
					} getOrElse {
						Redirect("/map").flashing("message" -> "Edit Sucessful")
					}
				}
			} recover {
				case _: Exception =>
					val form = editImageForm.fill(data)
					val flashSession = flash + ("message" -> "Failed to update image")
					InternalServerError(
						views.html.image(
							imageId,
							form
						)(flashSession, Some(user))
					)
			}
		}

		def failure(form: Form[EditImageFormData]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			BadRequest(
				views.html.image(
					imageId,
					form
				)(flash + ("message" -> error), Some(user))
			)
		}

		editImageForm.bindFromRequest().fold(failure, success)
	}	

	def imageToFormData(image: Image) = {
		import image._
		val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
		EditImageFormData(
			captureTime=time.map(t => df.format(new Date(t))),
			uploadTime=df.format(new Date(timeUploaded*1000L)),
			uploader=User.byId(user).email,
			onMap=(!pending).toString,
			latitude=lat,
			longitude=long,
			indicator=indicator,
			degree=degree,
			notes=notes
		)	
	}

	case class EditImageFormData(
		captureTime: Option[String],
		uploadTime: String,
		uploader: String,
		onMap: String,
		latitude: Option[Double],
		longitude: Option[Double],
		indicator: Int,
		degree: Int,
		notes: String
	)

	val editImageForm = Form(
		mapping(
			"capture_time" -> optional(text),
			"upload_time" -> text,
			"uploader" -> text,
			"onmap" -> text,
			"latitude" -> optional(doubleDecimal),
			"longitude" -> optional(doubleDecimal),
			"indicator" -> number,
			"degree" -> number,
			"notes" -> text
		)(EditImageFormData.apply)(EditImageFormData.unapply)
	)

	object Api {

		val addToCollectionForm = Form(
			tuple("collectionName" -> text, "imageIds" -> text)
		)
		def addToCollection() = authorizedAction(NormalUser) { implicit user => implicit request => 

			def success(values: (String, String)) = try {
				values match { case (collectionName, idsString) =>
					ensuringTransaction("default") { implicit trans =>
						val collection = Collection.withName(collectionName)
						val imageIds = Json.parse(idsString).as[Seq[String]].map(_.toLong)
						imageIds.foreach { imageId =>
							val image = Image(imageId)
							if (image.user != user.id && !user.isAdmin) throw new SQLException("Not your image")
							Image.addToCollection(imageId, collection.id)
						}
					}
					Redirect("/photoqueue").flashing("message" -> "Successfully added to collection")
				}
			} catch {
				case s: SQLException =>
					BadRequest("Bad Request")
			}

			def failure(form: Form[(String, String)]) = BadRequest("Bad Request")

			addToCollectionForm.bindFromRequest().fold(failure, success)

		}

		val createCollectionForm = Form("name" -> text)

		def createCollection = authorizedAction(AdminUser) { implicit user => implicit request =>

			def success(name: String) = try {
				val template = CollectionTemplate(name, user.id)
				Collection.insert(template)
				Redirect("/photoqueue").flashing("message" -> "Creation Successful")
			} catch {
				case e: SQLException => InternalServerError("Unable to create collection")
			}

			def failure(form: Form[String]) = BadRequest("Bad request")

			createCollectionForm.bindFromRequest().fold(failure, success)
		}

		val deleteCollectionForm = Form("name" -> text)

		def deleteCollection() = authorizedAction(AdminUser) { implicit user => implicit request =>
			def success(name: String) = try {
				Collection.deleteByName(name)
				Redirect("/photoqueue").flashing("message" -> "Deletion Successful")
			} catch {
				case e: SQLException => InternalServerError("Unable to delete collection")
			}

			def failure(form: Form[String]) = BadRequest("Bad request")

			deleteCollectionForm.bindFromRequest().fold(failure, success)
		}

		def delete(id: Long) = authorizedAction(NormalUser) { implicit user => implicit request => 
			Async {
				Akka.future {
					Image.delete(id)
					Redirect("/photoqueue").flashing("message" -> "Image was deleted")
				} recover {
					case _: Exception =>
						Redirect("/photoqueue").flashing("message" -> "Error deleting photo")
				}
			}
		}

		def uploadImage =
			authorizedAction(
				parse.multipartFormData,
				NormalUser
			) { implicit user => implicit request =>
				val formData = request.body.asFormUrlEncoded
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
							formData.get(name+"longitude").flatMap(_.headOption).map(_.toDouble) orElse metadata.latitude,
							formData.get(name+"longitude").flatMap(_.headOption).map(_.toDouble) orElse metadata.longitude,
							user.id,
							formData.get(name+"|time").flatMap(_.headOption).map(_.toLong) orElse metadata.time,
							formData.get(name+"|notes").flatMap(_.headOption).getOrElse(""),
							formData.get(name +"|indicator").flatMap(_.headOption).map(_.toInt).getOrElse(0),
							formData.get(name+"|degree").flatMap(_.headOption).map(_.toInt).getOrElse(0)
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
