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


	val reportForm = Form(
		mapping(
			"latitude" -> doubleDecimal,
			"longitude" -> doubleDecimal,
			"address" -> text,
			"location_description" -> text,
			"notes" -> text,
      "indicator" -> number,
      "degree" -> number
		)(ReportTemplate.apply)(ReportTemplate.unapply)
	)

	def createReport = Action { request =>
		val report = reportForm.bindFromRequest()(request).get
		Report.insert(report)	
		Ok(report.toString)
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



