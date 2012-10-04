
package modelSpec

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import models._

class CollectionSpec extends Specification {
	
	"The 'Collection' object" should {
		
		"start empty" in appCode {
			Collection.all.size must_== 0
		}

		"support a single insertion" in appCode {

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

			val imageId = Image.insert(insertedImage).get

			val insertedCollection = CollectionTemplate(
				"123 Snows Ave",
				"A snowy dwelling",
				"This is a fake location",
				1,
				3,
				imageId,
				Nil
			)

			val collectionId = Collection.insert(insertedCollection).get

			Collection.all.exists { dbCollection =>
				dbCollection.id == collectionId &&
				dbCollection.address == insertedCollection.address &&
				dbCollection.notes == insertedCollection.notes &&
				dbCollection.damageIndicator == insertedCollection.damageIndicator &&
				dbCollection.degreeOfDamage == insertedCollection.degreeOfDamage &&
				dbCollection.primaryImage == imageId
			}
		}

	}
	
}
