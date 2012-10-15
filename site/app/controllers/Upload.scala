
package controllers

import data.format.Formats._
import data.Forms._
import play.api.cache._
import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.MimeTypes
import models._

object Upload extends Controller{

	def page() = Cached("upload.page") {
		Action { Ok(views.html.upload()) }	
	}
}

