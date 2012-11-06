
package controllers

import play.api.mvc._
import models._
import jp.t2v.lab.play20.auth._
import java.io._
import play.api.libs.json._
import play.api.libs.json.Json.toJson

object Images extends Controller with Auth with AuthConfigImpl {

	def queuePage =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.photoQueue())	
		}

	def uploadPage =
		authorizedAction(NormalUser) { implicit user => implicit request =>
			Ok(views.html.upload()) 
		}	

	object Api {

		def addToCollection(
			imageId: Long,
			collectionId: Long,
			fromCollectionId: Option[Long]
		) = authorizedAction(NormalUser) { implicit user => implicit request => 
				Ok(Image.addToCollection(imageId, collectionId).toString)
		}

		def createCollection = Action(parse.urlFormEncoded) { request =>
			Ok(request.body.toString)	
			// get the id for all images selected
			val checkedRows =
				request.body.filter(row => row._2.contains("on")).keySet
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
				val fileStorageErrors = imageFiles.flatMap { file =>
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

			val unixTimestamp = date.flatMap(d => Option(d.getTime()));

			assert(latitude != null)
			assert(longitude != null)
			assert(unixTimestamp != null)

			ImageMetadata(latitude, longitude, unixTimestamp)
		} catch {
			case ipe: ImageProcessingException => ImageMetadata(None, None, None)
		}
	}
}
