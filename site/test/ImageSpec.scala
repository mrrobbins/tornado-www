
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play

import models.Image

class ImageSpec extends Specification {
	val fa = FakeApplication(additionalConfiguration = inMemoryDatabase())
	def withApp[T](func: => T): T = {
		Play.start(fa)
		try {
			func
		} finally {
			Play.stop()
		}
	}
	"The 'Image' object" should {
		"start empty" in {
			withApp {
				Image.all.size must_== 0
			}
		}
	}
}
