package models

import java.io.File
import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.Play
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration

trait ImageHandler {

	def store(file: FilePart[TemporaryFile]): String
	def lookupImage(key: String): String
	def lookupThumb(key: String): String

}

object ImageHandler {

	private val handler = {
		val backendConfig = configuration.getConfig("data.backend").getOrElse {
			Logger.error("No data backend configuration present. Please set configure a data backend in application.conf")
			Play.stop()
			sys.exit(1)
		}

		val backendClassName = backendConfig.getString("name").getOrElse {
			Logger.error("No data backend set. Please set a data backend name in application.conf")
			Play.stop()
			sys.exit(1)
		}

		try {
			val backendClass = Class.forName(backendClassName)
			val constructor = backendClass.getConstructor(classOf[Configuration])
			constructor.newInstance(backendConfig).asInstanceOf[ImageHandler]
		} catch {
			case e: Exception =>
				Logger.error("Error configuring data backend", e)
				Play.stop()
				sys.exit(1)
		}
	}

	def apply() = handler
}

