
package models

import java.io.File
import scala.util.Random
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import data.FileTypes._
import play.api.Configuration

class LocalImageHandler(c: Configuration) extends ImageHandler {
	val rand = new Random(System.currentTimeMillis())
	val path = c.getString("storagepath").getOrElse(throw new Exception("No storagepath specified. Please specify a storagepath in application.conf"))

	def store(file: FilePart[TemporaryFile]): String = {
		val fileExt = file.contentType.flatMap(Extensions.get).getOrElse {
			file.filename.split("\\.").drop(1).lastOption.getOrElse("")
		}

		val maxNumber = 1000000000000l
		val potentialNames = Stream.continually {
			var filename = (rand.nextDouble()*maxNumber).toLong
			new File(path + filename + "."+fileExt)
		}

		// build a list of many distinct integers as candidate file names
		// get a File path that doesn't already exist
		// will fail after Int.MaxValue if none are found
		val outputFile = potentialNames.distinct.take(Int.MaxValue).find(!_.exists).get
		file.ref.moveTo(outputFile)
		val outputFilename = outputFile.getName
		ImageThumbnailer.createThumbnail(outputFile, outputFilename)
		outputFilename
	}

	def lookupImage(key: String): String = {
		"/image/" + key
	}

	def lookupThumb(key: String): String = {
		ImageThumbnailer.lookup(key)
	}
}

