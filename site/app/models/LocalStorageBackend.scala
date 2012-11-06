
package models

import java.io.File
import scala.util.Random
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import data.FileTypes._
import play.api.Configuration

class LocalStorageBackend(c: Configuration) extends StorageBackend {
	val rand = new Random(System.currentTimeMillis())
	val path = c.getString("path").getOrElse {
		throw new Exception(
			"No storage path specified. Please specify a path in application.conf"
		)
	}

	def store(
		bucket: String,
		file: File,
		contentType: Option[String]=None,
		name: Option[String]=None
	): String = {
		val prefix = path + "/storage/" + bucket + "/"
		val prefixFile = new File(prefix)
		if (!prefixFile.exists()) prefixFile.mkdirs()
		val outputFile = name.map(n => new File(prefixFile, n)).getOrElse {
			val fileExt = contentType.flatMap(Extensions.get).getOrElse {
				file.getName.split("\\.").drop(1).lastOption.getOrElse("")
			}

			val maxNumber = 1000000000000l
			val potentialNames = Stream.continually {
				var filename = (rand.nextDouble()*maxNumber).toLong
				new File(prefixFile, filename + "." + fileExt)
			}

			// build a list of many distinct integers as candidate file names
			// get a File path that doesn't already exist
			// will fail after Int.MaxValue if none are found
			potentialNames.distinct.take(Int.MaxValue).find(!_.exists).getOrElse {
				throw new Exception("Could not create unique filename")
			}
		}
		file.renameTo(outputFile)
		outputFile.getName
	}

	def lookup(bucket: String, key: String): String = {
		"/data/storage/" + bucket + "/" + key
	}

	def fetch(bucket: String, key: String): File = {
		new File(path + "/" + bucket + "/" + key)
	}

}

