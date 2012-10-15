package models

import java.io.File
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

trait ImageHandler {

	def store(file: FilePart[TemporaryFile]): String
	def lookup(key: String): String

}

