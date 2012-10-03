
import play.api.test._
import play.api.test.Helpers._
import play.api.Play

package object modelSpec {

	def appCode[T](func: => T): T = {
		Play.start(FakeApplication(additionalConfiguration = inMemoryDatabase()))
		try {
			func
		} finally {
			Play.stop()
		}
	}
}
