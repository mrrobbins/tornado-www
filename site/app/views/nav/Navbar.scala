
package views.nav

case class NavItem(name: String, url: String)

object NavBar {

	object Map extends NavItem("map", "/map")
	object PhotoQueue extends NavItem("photo queue", "/photoqueue")

	val headerItems = Seq(Map, PhotoQueue)
}
