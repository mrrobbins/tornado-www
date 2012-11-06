
package controllers

import play.api.mvc._
import jp.t2v.lab.play20.auth._
import models._
import play.api.libs.json._
import play.api.libs.json.Json.toJson
import play.api.libs.concurrent.Akka
import play.api.Play.current

object Maps extends Controller with Auth with AuthConfigImpl {

  def page = optionalUserAction { implicit maybeUser => implicit request =>
			Ok(views.html.map())
	}

	object Api {

		def balloon(imageId: Long) = Action { Async { Akka.future {
				val image = Image.all.filter(image => image.id == imageId).head
				val content = views.html.balloon(image).toString
		
				val contentJson = toJson(Map("content" -> toJson(content)))

				Ok(contentJson)	
		} } }

		def allMarkers = Action { Async { Akka.future {

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
		} } }
	}

}
