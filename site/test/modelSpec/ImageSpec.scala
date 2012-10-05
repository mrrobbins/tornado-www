
package modelSpec

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play
import play.api.Play.current
import play.api.db.DB

import models._

class ImageSpec extends Specification {

	"The 'Image' object" should {
		"start empty" in appCode {
				Image.all.size must_== 0
		}

		"support a single insertion" in appCode {

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

		"support multiple insertions" in appCode {

			val insertedUser = UserTemplate("al", false)
			val userId = User.insert(insertedUser)

			val images = for (i <- 1 to 100) yield {
				ImageTemplate(
					"/path/i",
					i * 0.5,
					i * 1.1,
					userId,
					"Notes " + i,
					i % 5,
					i % 5
				)
			}

			images.foreach(Image.insert)
			images.forall { template =>
				Image.all.exists { image =>
					image.lat == template.lat &&
					image.long == template.long &&
					image.notes == template.notes &&
					image.path == template.path &&
					image.indicator == template.indicator &&
					image.degree == template.degree
				}
			}
			
		}

		"add new images as pending" in appCode {

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

			val imageId = Image.insert(insertedImage)

			val fromDB = Image.pending(imageId).get
			fromDB.pending && fromDB.id == imageId && fromDB.user == userId

		}

		"support adding images to a collection" in appCode {
	
			val insertedUser = UserTemplate("al", false)
			val userId = User.insert(insertedUser)

			val insertedImage1 = ImageTemplate(
				"/path1",
				3.4,
				2.3,
				userId,
				"Notes1",
				9,
				1
			)

			val firstImage = Image.insert(insertedImage1)

			val insertedCollection = CollectionTemplate(
				"234 Breezy Way",
				"A large house",
				"It's windy here",
				4,
				2,
				firstImage,
				Nil
			)

			val collectionId = Collection.insert(insertedCollection)

			val insertedImage2 = ImageTemplate(
				"/path2",
				1.2,
				5.3,
				userId,
				"Notes",
				7,
				8
			)

			val secondImage = Image.insert(insertedImage2)
			Image.addToCollection(secondImage, collectionId)

			Image.all.exists { image =>
				image.id == secondImage &&
				image.collectionId == Some(collectionId) &&
				image.path == insertedImage2.path &&
				image.notes == insertedImage2.notes &&
				image.lat == insertedImage2.lat &&
				image.long == insertedImage2.long &&
				image.user == userId &&
				image.indicator == insertedImage2.indicator &&
				image.degree == insertedImage2.degree
			}
		}
	}
}
