
package modelSpec

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play

import models._

class UserSpec extends Specification {


	"The 'User' object" should {
		"start empty" in withApp {
			User.all.size must_== 0
		}

		"support a single insertion" in withApp {

			val inserted = UserTemplate("al", false)
			val id = User.insert(inserted)
			User.all.exists { user =>
				user.name == inserted.name &&
				user.id == id &&
				user.isAdmin == inserted.isAdmin
			}

		}
	}
	
}

