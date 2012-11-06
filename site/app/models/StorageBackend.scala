package models

import java.io.File
import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.Play
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration

trait StorageBackend {

	def store(
		bucket: String,
		file: File,
		contentType: Option[String]=None,
		name: Option[String]=None
	): String
	def lookup(bucket: String, key: String): String

}

object StorageBackend {

	private val handler = {
		val backendConfig = configuration.getConfig("storage").getOrElse {
			Logger.error("No storage backend configuration present. Please set configure a storage backend in application.conf")
			Play.stop()
			sys.exit(1)
		}

		val backendClassName = backendConfig.getString("backend.name").getOrElse {
			Logger.error("No storage backend set. Please set a data backend name in application.conf")
			Play.stop()
			sys.exit(1)
		}

		try {
			val backendClass = Class.forName(backendClassName)
			val constructor = backendClass.getConstructor(classOf[Configuration])
			constructor.newInstance(backendConfig).asInstanceOf[StorageBackend]
		} catch {
			case e: Exception =>
				Logger.error("Error configuring storage backend", e)
				Play.stop()
				sys.exit(1)
		}
	}

	def apply() = handler
}

