
package models

import play.api.Play.current
import java.sql._
import anorm._
import play.api.db.DB

object ModelHelpers {

	def ensuringConnection[T](func: Connection => T)(implicit conn: Connection): T = {
		if (conn == null) DB.withConnection(implicit c => func(c))
		else func(conn)
	}

	def ensuringTransaction[T](func: Connection => T)(implicit trans: Connection): T = {
		if (trans == null || trans.getAutoCommit) DB.withTransaction(t => func(t))
		else func(trans)
	}

}
