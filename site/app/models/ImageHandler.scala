package models

import java.io.File
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

trait ImageHandler {

	def store(file: FilePart[TemporaryFile]): String
	def lookupImage(key: String): String
	def lookupThumb(key: String): String

}

