
package models

case class PendingImage(
	imageId: Long,
	notes: String,
	indicator: Int,
	degree: Int
)

object PendingImage {
	def withId(id: Long) = PendingImage(id, "", 0, 0)
	def remove(id: Long) = 0
}
