package controllers

import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.cache._
import models._
import jp.t2v.lab.play20.auth._

object Application extends Controller with Auth with AuthConfigImpl {
  
  def index = Action {
		Redirect(routes.Application.map)
  }

  /*def map = Cached("map.html") {*/
  def map = optionalUserAction { implicit maybeUser => implicit request =>
			Ok(views.html.map())
	}

	def photoQueue = authorizedAction(NormalUser) { implicit user => implicit request =>
		Ok(views.html.photoQueue())	
	}
  
}
