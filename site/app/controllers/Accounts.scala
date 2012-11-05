package controllers

import play.api.mvc._
import jp.t2v.lab.play20.auth._
import play.api.data.Forms._
import play.api.data.Form
import models.User
import models.UserTemplate
import play.api.libs.json._

object Accounts extends Controller with LoginLogout with Auth with AuthConfigImpl {
	def loginForm() = optionalUserAction { implicit maybeUser => implicit request =>
		Ok(views.html.login())
	}

	def loginSubmit() = Action { implicit request => 
		def success(form: (String, String)) = {
			import form._
			try {
				val user = User(form._1, form._2)
				gotoLoginSucceeded(user.id)
			} catch {
				case _: Exception => Redirect("/login").flashing("message" -> "Invalid username or password", "styleClass" -> "red")
			}
		}
		def failure(form: Form[(String, String)]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			Redirect("/login").flashing("message" -> error)
		}

		userLoginForm.bindFromRequest().fold(failure, success)
	}

	def signupForm() = optionalUserAction { implicit maybeUser => implicit request =>
		Ok(views.html.signup())
	}

	val userLoginForm = Form(
		tuple(
			"email" -> email,
			"password" -> text
		)
	)

	val createAccountForm = Form(
		tuple(
			"fname" -> text,
			"lname" -> text,
			"email" -> email,
			"email2" -> email,
			"password" -> text,
			"password2" -> text
		).verifying(
			"Name is invalid",
			t => t._1.forall(c => !Character.isISOControl(c)) &&
			t._2.forall(c => !Character.isISOControl(c)) &&
			t._1.length > 0 &&
			t._2.length > 0	
		).verifying(
			"Emails do not match",
			t => t._3 == t._4
		).verifying(
			"Passwords do not match",
			t => t._5 == t._6
		).verifying(
			"Passwords must be at least 8 characters",
			t => t._5.length >= 8	
		).verifying(
			"Name is invalid",
			t => t._1.forall(c => !Character.isISOControl(c)) &&
			t._2.forall(c => !Character.isISOControl(c)) &&
			t._1.length > 0 &&
			t._2.length > 0	
		) 
	)

	def signupSubmit() = Action { implicit request =>
		def success(form: (String, String, String, String, String, String)) = {
			import form._
			val newUser = UserTemplate(_3, _5, _1, _2, false)
			try {
				User.insert(newUser)
				Redirect("/login").flashing("message" -> "Account succesfully created", "styleClass" -> "green")
			} catch {
				case _: Exception => Redirect("/signup").flashing("message" -> "Failed to create account")
			}
		}
		def failure(form: Form[(String, String, String, String, String, String)]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			//BadRequest(views.html.signup(Some(form))).flashing("message" -> error)
			Redirect("/signup").flashing("message" -> error)
		}

		createAccountForm.bindFromRequest().fold(failure, success)
	}
}
