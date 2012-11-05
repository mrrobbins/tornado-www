
package views.nav

case class NavItem(name: String, url: String)

object NavBar {

	object Map extends NavItem("map", "/map")
	object PhotoQueue extends NavItem("photo queue", "/photoqueue")
	object PhotoUpload extends NavItem("upload", "/upload")
	object Login extends NavItem("login", "/login")

	val headerItems = Seq(Map, PhotoUpload, PhotoQueue)
}
