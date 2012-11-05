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

	case class LoginFormData(email: String, password: String)

	def loginSubmit() = Action { implicit request => 
		def success(data: LoginFormData) = {
			try {
				val user = User(data.email, data.password)
				gotoLoginSucceeded(user.id)
			} catch {
				case _: Exception =>
					val flashSession = flash + ("message" -> "Invalid email or password") + ("styleClass" -> "red")
					val form = Some(userLoginForm.fill(data.copy(password="")))
					BadRequest(views.html.login(form)(flashSession))
			}
		}
		def failure(form: Form[LoginFormData]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			val flashSession= flash + ("message" -> error) + ("styleClass" -> "red")
			InternalServerError(views.html.login(Some(form))(flashSession))
		}

		userLoginForm.bindFromRequest().fold(failure, success)
	}

	def signupForm() = optionalUserAction { implicit maybeUser => implicit request =>
		Ok(views.html.signup())
	}

	val userLoginForm = Form(
		mapping(
			"email" -> email,
			"password" -> text
		)(LoginFormData.apply)(LoginFormData.unapply)
	)

	case class SignupFormData(
		fname: String,
		lname: String,
		email: String,
		email2: String,
		password: String,
		password2: String
	)

	val createAccountForm = Form(
		mapping(
			"fname" -> text,
			"lname" -> text,
			"email" -> email,
			"email2" -> email,
			"password" -> text,
			"password2" -> text
		)(SignupFormData.apply)(SignupFormData.unapply).verifying(
			"Name is invalid",
			data => data.fname.forall(c => !Character.isISOControl(c)) &&
			data.lname.forall(c => !Character.isISOControl(c)) &&
			data.fname.length > 0 &&
			data.lname.length > 0	
		).verifying(
			"Emails do not match",
			data => data.email == data.email2
		).verifying(
			"Passwords do not match",
			data => data.password == data.password2
		).verifying(
			"Passwords must be at least 8 characters",
			data => data.password.length >= 8	
		)
	)

	def signupSubmit() = Action { implicit request =>
		def success(data: SignupFormData) = {
			import data._
			val newUser = UserTemplate(email, password, fname, lname, false)
			try {
				User.insert(newUser)
				Redirect("/login").flashing("message" -> "Account succesfully created", "styleClass" -> "green")
			} catch {
				case _: Exception =>
					val form = Some(createAccountForm.fill(data))
					val flashSession = flash + ("message" -> "Failed to create account")
					InternalServerError(views.html.signup(form)(flashSession))
			}
		}
		def failure(form: Form[SignupFormData]) = {
			val json = form.errorsAsJson.asInstanceOf[JsObject]
			val error = json \ "" match {
				case _: JsUndefined => (json \ json.keys.head)(0).as[String]
				case value: JsArray => value(0).as[String]
				case _ => "Bad data"
			}
			BadRequest(views.html.signup(Some(form))(flash + ("message" -> error)))
		}

		createAccountForm.bindFromRequest().fold(failure, success)
	}
}
