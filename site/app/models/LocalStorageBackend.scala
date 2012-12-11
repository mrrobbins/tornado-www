
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

	val prefix = new File(path + "/storage")

	def store(
		bucket: String,
		file: File,
		contentType: Option[String]=None,
		name: Option[String]=None
	): String = {
		val prefixFile = new File(prefix, bucket)
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
		val moveSuccessful = file.renameTo(outputFile)
		if (!moveSuccessful) throw new Exception("Could not move file to destination")
		outputFile.getName
	}

	def lookup(bucket: String, key: String): String = {
		"/data/storage/" + bucket + "/" + key
	}

	def delete(bucket: String, key: String): Boolean = {
		val file = new File(prefix, bucket + "/" + key)
		file.delete()
	}
}

