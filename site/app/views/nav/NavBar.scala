
package views.nav

import models._

case class NavItem(name: String, url: String)

object NavBar {

	object Map extends NavItem("map", "/map")
	object PhotoQueue extends NavItem("photo queue", "/photoqueue")
	object PhotoUpload extends NavItem("upload", "/upload")
	object Login extends NavItem("login", "/login")

	val headerItems = Seq(Map, PhotoUpload, PhotoQueue)
}

sealed trait NavOptUser {
	val maybeUser: Option[User]
}

object NavOptUser {
	private final class UserOptUser(u: User) extends NavOptUser {
		val maybeUser = Some(u)
	}
	private final class OptUser(o: Option[User]) extends NavOptUser {
		val maybeUser = o
	}

	implicit def wrapUser(implicit u: User): NavOptUser =
		new UserOptUser(u)
	implicit def direct(implicit o: Option[User]): NavOptUser =
		new OptUser(o)

}

