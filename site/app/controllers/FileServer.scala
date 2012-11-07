
package controllers

import play.api.Play
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration
import java.io.File
import models._
import play.api.mvc._
import play.api.libs.concurrent.Akka

object FileServer extends Controller{

	val storageDirectory = {
		val path = configuration.getString("storage.path").getOrElse {
			Logger.error(
				"No storage path specified. Please specify a path in application.conf"
			)
			Play.stop()
			sys.exit(1)
		}
		val file = new File(path)
		if (!file.exists) file.mkdirs()
		file
	}.getCanonicalFile
	
	/* Serve a file from the storage directory
	 * @param a path within the storage directory
	 */
	def serve(path: String) = Action { 
		// if path contains ../ notation, could request file outside of valid dirs
		// construct a file handle from storageDirectory and path
		val file = new File(storageDirectory, path).getCanonicalFile

		val parents = Stream.iterate(file)(_.getParentFile).takeWhile(_ != null)
		// verify file's path still child of storageDirectory (request is within valid dirs)
		if (parents contains storageDirectory) {
			Async { Akka.future {
				Ok.sendFile(
					content = new File(storageDirectory, path)
				)
			} }
		} else {
			Forbidden("Access Denied")
		}
	}

}

