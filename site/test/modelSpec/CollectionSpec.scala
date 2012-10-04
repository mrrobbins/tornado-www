
package modelSpec

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import anorm._

import models._

class CollectionSpec extends Specification {
	
	/*** Alias UNIX_TIMESTAMP for h2
	*/
	def insertTimestampAlias() = {
		DB.withConnection { conn =>
			val query = SQL(
				"""
					CREATE ALIAS UNIX_TIMESTAMP AS $$
						long unix_timestamp() { return System.currentTimeMillis()/1000L; }
					$$;

				"""
			)

			query.execute()(conn)
		}
	}

	"The 'Collection' object" should {
		
		"start empty" in appCode {
			Collection.all.size must_== 0
		}

		"support a single insertion" in appCode {

			insertTimestampAlias()

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
