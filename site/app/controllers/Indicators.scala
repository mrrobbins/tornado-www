
package controllers

import play.api.Play.current
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json._

import models._

object Indicators extends Controller {

	object Api {
		def degrees(id: Int) = Action { implicit request =>
			val indicators = DamageIndicator.all
			val maybeIndicator: Option[DamageIndicator] = indicators.find(_.id == id)
			val degrees = maybeIndicator.map(_.degrees).getOrElse(Nil)
			val objects = degrees.map { degree =>
				toJson(Map(
					"id" -> toJson(degree.id),
					"description" -> toJson(degree.description)
				))
			}
			Ok(toJson(objects))
		}
	}
	
}

