
package models

import ModelHelpers._
import anorm._
import play.api.db.DB
import play.api.Play.current

case class Marker(
	description: String,
	latitude: Double,
	longitude: Double
)

object Marker {
	
	def all: List[Marker] = {
		Image.all.filter(!_.pending).map { image =>
			Marker(
				image.notes,
				image.lat,
				image.long
			)
		}
	}

}

