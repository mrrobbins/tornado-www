package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.Report

object Api extends Controller {


	val reportForm = Form(
		mapping(
			"id" -> optional(number),
			"latitude" -> text,
			"longitude" -> text,
			"address" -> text,
			"location_description" -> text,
			"notes" -> text
		)
		(formToReport)
		(reportToForm)
	)

	private def reportToForm(r: Report) = {
		Some(
			r.id, 
			r.latitude.toString, 
			r.longitude.toString, 
			r.address, 
			r.locationDescription, 
			r.notes
		)
	}

	private def formToReport(id: Option[Int], lat: String, long: String, address: String, locDesc: String, notes: String) = {
		Report(
			id, 
			if (!lat.isEmpty) lat.toDouble else 0, 
			if (!long.isEmpty) long.toDouble else 0, 
			address,
			locDesc,
			notes
		) 
	}

	def createReport = Action { request =>
		val report = reportForm.bindFromRequest()(request).get
		Report.insert(report)	
		Ok(report.toString)
	}

}

