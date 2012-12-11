package controllers

import play.api.Play.current
import play.api.mvc._
import jp.t2v.lab.play20.auth._
import play.api.data.Forms._
import play.api.data.Form
import models.User
import models.UserTemplate
import play.api.libs.json._
import play.api.libs.concurrent.Akka

object About extends Controller {

	/** Handles requests for the about page
	  */
	def page() = Action { implicit request =>
		Ok(views.html.about())
	}

}
