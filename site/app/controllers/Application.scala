package controllers

import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.cache._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def map = Cached("map.html") {
		Action {
			Ok(views.html.map())
		}
	}
  
}
