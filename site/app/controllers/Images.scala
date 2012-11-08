
package controllers

import play.api.mvc._
import models._
import jp.t2v.lab.play20.auth._
import java.io._
import play.api.libs.json._
import play.api.libs.json.Json.toJson
import play.api.libs.concurrent.Akka
import play.api.Play.current

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
			Ok(views.html.image(Image(imageId))) 
		}	

	def editSubmit =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok("success") 
		}	

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
					val result = Json.toJson(fileStorageErrors++fileTypeErrors)
					Ok(result)
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
