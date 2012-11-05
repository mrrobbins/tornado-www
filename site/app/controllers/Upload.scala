
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
import jp.t2v.lab.play20.auth._

object Upload extends Controller with Auth with AuthConfigImpl {

	/*def page = Cached("upload.page") {*/
	def page = authorizedAction(NormalUser) { implicit user => implicit request =>
		Ok(views.html.upload()) 
	}	
}

