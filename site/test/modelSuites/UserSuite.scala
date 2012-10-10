
package test.modelSuite

import org.scalatest.FunSuite

import test.FakeApp

import models._


class UserSpec extends FunSuite with FakeApp {

	test("starts empty") {
		assert(User.all.size === 0)
	}

	test("supports a single insertion") {

		val inserted = UserTemplate("al", false)
		val id = User.insert(inserted)
		assert(User.all.exists { user =>
			user.name == inserted.name &&
			user.id == id &&
			user.isAdmin == inserted.isAdmin
		})

	}
	
}

