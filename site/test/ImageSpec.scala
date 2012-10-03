
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play

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

			val insertedUser = User(88, "al", false)

			val insertedImage = ImageTemplate(
				"/path",
				3.4,
				2.3,
				insertedUser.id,
				"Notes",
				9,
				1
			)

			User.insert(insertedUser)
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

	}
}
