
package test

import play.api.test._
import play.api.test.Helpers._
import play.api.Play
import play.api.Play.current
import play.api.db._
import anorm._

import org.scalatest.SuiteMixin
import org.scalatest.Suite

trait FakeApp extends SuiteMixin { self: Suite =>

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


	abstract override def withFixture(test: NoArgTest) {
		Play.start(FakeApplication(additionalConfiguration = inMemoryDatabase()))
		try {
			insertTimestampAlias()
			super.withFixture(test)
		} finally {
			Play.stop()
		}
	}

}
