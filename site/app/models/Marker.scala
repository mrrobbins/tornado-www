
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current

case class Marker(
	id: Long,
	description: String,
	latitude: Option[Double],
	longitude: Option[Double],
	markerType: String
)

object Marker {
	
	def all: List[Marker] = {
		val pending = Image.all().filter(!_.pending).map { image =>
			Marker(
				image.id,
				image.notes,
				image.lat,
				image.long,
				"standardImage"
			)
		}
		pending
	}

}

