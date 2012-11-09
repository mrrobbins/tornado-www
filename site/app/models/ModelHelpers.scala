
package models

import play.api.Play.current
import java.sql._
import anorm._
import play.api.db.DB

object ModelHelpers {

	def ensuringConnection[T](database: String)(func: Connection => T)(implicit conn: Connection): T = {
		if (conn == null) DB.withConnection(func)
		else func(conn)
	}

	def ensuringConnection[T](func: Connection => T)(implicit conn: Connection): T =
		ensuringConnection("default")(func)(conn)

	def ensuringTransaction[T](database: String)(func: Connection => T)(implicit trans: Connection): T = {
		if (trans == null || trans.getAutoCommit) DB.withTransaction { t =>
				try {
					val res = func(t)
					t.commit()
					res
				} catch {
					case e: Exception =>
						t.rollback()
						throw e
				}
		} else func(trans)
	}

	def ensuringTransaction[T](func: Connection => T)(implicit trans: Connection): T =
		ensuringTransaction("default")(func)(trans)

}
