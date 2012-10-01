
package models

case class Image(
	id: Long,
	path: String,
	time: Long,
	lat: Double,
	long: Double,
	user: Int,
	notes: String,
	indicator: Int,
	degree: Int
) {

	def setCollection(collectionId: Long) { Image.setCollection(id, collectionId) }

}

object Image {
	def setCollection(imageId: Long, collectionId: Long) { }
}
