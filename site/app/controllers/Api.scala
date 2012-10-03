
package controllers

import data.format.Formats._
import data.Forms._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import models._

object Api extends Controller {

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
		import Json.toJson

		val allMarkers = Marker.all.map(marker =>
			Map(
				"name" -> toJson(marker.description), 
				"lat" -> toJson(marker.latitude), 
				"long" -> toJson(marker.longitude)
			)
		)

		val allMarkersJson = toJson(Map("markers" -> toJson(allMarkers)))

		Ok(allMarkersJson)
	}

}

