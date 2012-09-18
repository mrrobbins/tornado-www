package controllers

import data.format.Formats._
import data.Forms._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import models.Report

object Api extends Controller {


	val reportForm = Form(
		mapping(
			"id" -> optional(longNumber),
			"latitude" -> doubleDecimal,
			"longitude" -> doubleDecimal,
			"address" -> text,
			"location_description" -> text,
			"notes" -> text
		)(Report.apply)(Report.unapply)
	)

	def createReport = Action { request =>
		val report = reportForm.bindFromRequest()(request).get
		Report.insert(report)	
		Ok(report.toString)
	}

}

