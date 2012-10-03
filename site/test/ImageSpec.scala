
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play
import play.api.Play.current
import play.api.db.DB

import models._

class ImageSpec extends Specification {

	def fa = FakeApplication(additionalConfiguration = inMemoryDatabase())

	def withApp[T](func: => T): T = {
		Play.start(fa)
		try {
			func
		} finally {
			Play.stop()
		}
	}

	"The 'Image' object" should {
		"start empty" in withApp {
				Image.all.size must_== 0
		}

		"support a single insertion" in withApp {

			val insertedUser = UserTemplate("al", false)

			val userId = User.insert(insertedUser)

			val insertedImage = ImageTemplate(
				"/path",
				3.4,
				2.3,
				userId,
				"Notes",
				9,
				1
			)

			Image.insert(insertedImage)

			Image.all.exists { image =>
				image.lat == insertedImage.lat &&
				image.long == insertedImage.long &&
				image.notes == insertedImage.notes &&
				image.path == insertedImage.path &&
				image.indicator == insertedImage.indicator &&
				image.degree == insertedImage.degree
			}

		}

		"add new images as pending" in withApp {

			val insertedUser = UserTemplate("al", false)
			val userId = User.insert(insertedUser)

			val insertedImage = ImageTemplate(
				"/path",
				3.4,
				2.3,
				userId,
				"Notes",
				9,
				1
			)

			val imageId = Image.insert(insertedImage).get

			val fromDB = DB.withConnection(conn => Image.pending(conn)(imageId)).get
			fromDB.pending && fromDB.id == imageId && fromDB.user == userId

		}

	}
}
