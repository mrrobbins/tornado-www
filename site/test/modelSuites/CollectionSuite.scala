
package test.modelSuites

import org.scalatest.FunSuite

import test.FakeApp

import models._

class CollectionSuite extends FunSuite with FakeApp {
	
	test("starts empty") {
		assert(Collection.all.size === 0)
	}

	test("supports a single insertion") {

		val insertedUser = UserTemplate("al", true)
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

		val imageId = Image.insert(insertedImage)

		val insertedCollection = CollectionTemplate(
			"123 Snows Ave",
			"A snowy dwelling",
			"This is a fake location",
			1,
			3,
			imageId,
			Nil
		)

		val collectionId = Collection.insert(insertedCollection)

		assert(Collection.all.exists { dbCollection =>
			dbCollection.id == collectionId &&
			dbCollection.address == insertedCollection.address &&
			dbCollection.notes == insertedCollection.notes &&
			dbCollection.damageIndicator == insertedCollection.damageIndicator &&
			dbCollection.degreeOfDamage == insertedCollection.degreeOfDamage &&
			dbCollection.primaryImage == imageId
		})
	}

}
