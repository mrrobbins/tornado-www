
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

	def imageUpload = Action(parse.multipartFormData) { request =>
		// remove missing or non-image mime types
		val (imageFiles, errorFiles) = request.body.files.span(_.contentType.filter(_.startsWith("image/")).isDefined)
		val fileNames = imageFiles.map { file =>
			val size = file.ref.file.length
			val name = imgHandler.store(file)
			(name, size)
		}

		val imageFileResponses = fileNames.map { case (name, size) => 
			Json.toJson(Map(
				"name" -> toJson(name),
				"size" -> toJson(size),
				"url" -> toJson(imgHandler.lookup(name)),
				"thumbnail_url" -> toJson(imgHandler.lookup(name))
			))
		}

		val errorFileResponses = errorFiles.map { file =>
			Json.toJson(Map(
				"name" -> toJson(file.filename),
				"error" -> toJson("Not an image file!")
			))
		}

		val result = Json.toJson(imageFileResponses++errorFileResponses)

		Ok(result)
	}
}

