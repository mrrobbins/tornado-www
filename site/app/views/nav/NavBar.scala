
package views.nav

case class NavItem(name: String, url: String)

object NavBar {

	object Map extends NavItem("map", "/map")
	object PhotoQueue extends NavItem("photo queue", "javascript:void(0)")
	object PhotoUpload extends NavItem("upload", "/upload")

	val headerItems = Seq(Map, PhotoUpload, PhotoQueue)
}
