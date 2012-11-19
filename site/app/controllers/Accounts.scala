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

object Accounts extends Controller with LoginLogout with Auth with AuthConfigImpl {


	/** Handles requests for the login page
	  */
	def login() = Action { implicit request =>
		Ok(views.html.login())
	}

  /** Holds the data from a login form
    * @param email the email address that the user supplied
    * @param password the password that the user supplied (unhashed! don't store!)
    */
	case class LoginFormData(email: String, password: String)

	def loginSubmit() = Action { implicit request => 
		def success(data: LoginFormData) = Async {
			Akka.future {
				val user = User(data.email, data.password)
				gotoLoginSucceeded(user.id)
			} recover {
				case _: Exception =>
					val flashSession = flash + ("message" -> "Invalid email or password") + ("styleClass" -> "red")
					val form = userLoginForm.fill(data.copy(password=""))
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

			//Remove password field from form (security)
			val formValues = form.data - "password"
			val modForm = form.bind(formValues)
			InternalServerError(views.html.login(modForm)(flashSession))
		}

		userLoginForm.bindFromRequest().fold(failure, success)
	}

	def signup() = Action { implicit request =>
		Ok(views.html.signup())
	}

	def logoutSubmit = Action { implicit request =>
		gotoLogoutSucceeded
	}


	val userLoginForm = Form(
		mapping(
			"email" -> email,
			"password" -> text
		)(LoginFormData.apply)(LoginFormData.unapply)
	)

  /** Holds the data from a signup form
    * @param fname the supplied first name
    * @param lname the supplied last name
    * @param email the supplied email address
    * @param email2 the supplied email address confirmation
    * @param password the password that the user supplied (unhashed! don't store!)
    * @param password2 the supplied password confirmation (unhashed! don't store!)
    */
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
		def success(data: SignupFormData) = Async {
			import data._
			val newUser = UserTemplate(email, password, fname, lname, false)
			Akka.future {
				User.insert(newUser)
				Redirect("/login").flashing("message" -> "Account succesfully created", "styleClass" -> "green")
			} recover {
				case _: Exception =>
					val form = createAccountForm.fill(data)
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
			BadRequest(views.html.signup(form)(flash + ("message" -> error)))
		}

		createAccountForm.bindFromRequest().fold(failure, success)
	}
}
