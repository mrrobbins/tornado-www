
package models

import java.io.File
import scala.util.Random
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import data.FileTypes._
import scala.io.Codec.ISO8859
import play.api.Configuration
import fly.play.aws.auth.SimpleAwsCredentials
import fly.play.s3._
import data.FileTypes
import java.io.IOException

class S3StorageBackend(c: Configuration) extends StorageBackend {
	val rand = new Random(System.currentTimeMillis())
	val accessKeyId = c.getString("aws.accessKeyId").getOrElse {
		throw new Exception("Required Access Key ID not found"
		)
	}
	val secretKey = c.getString("aws.secretKey").getOrElse {
		throw new Exception("Required Secret Key not found")
	}

	implicit val credentials = SimpleAwsCredentials(accessKeyId, secretKey)

	val bucketPrefix = "damage-tracker-"

	def store(
		bucket: String,
		file: File,
		contentType: Option[String]=None,
		name: Option[String]=None
	): String = {
		val b = S3(bucketPrefix + bucket)

		val fileExt = contentType.flatMap(Extensions.get).getOrElse {
			file.getName.split("\\.").drop(1).lastOption.getOrElse("")
		}

		val potentialNames = name ++: Stream.continually {
			val maxNumber = 1000000000000l
			var filename = (rand.nextDouble()*maxNumber).toLong
			filename + "." + fileExt
		}
		
		val source = scala.io.Source.fromFile(file)(ISO8859)
		val byteArray = source.map(_.toByte).toArray
		source.close()

		val mimeType = contentType.getOrElse(FileTypes.MimeTypes.get(fileExt).getOrElse(""))

		val outputFile = potentialNames.distinct.take(Int.MaxValue).find{ fileName =>
			val result = b add BucketFile(fileName, mimeType, byteArray)	
			result.map { 
				case Left(error) => false
				case Right(success) => true
			}.await(20000).get
		}

		outputFile.getOrElse(throw new IOException("File not created"))
	}
			
	def lookup(bucket: String, key: String): String = {
		val b = S3(bucketPrefix + bucket)
		b.url(key, 300)
	}

	def delete(bucket: String, key: String): Boolean = {
		val b = S3(bucketPrefix + bucket)
		val result = b remove key
		result.map {
			case Left(error) => false
			case Right(success) => true
		}.await(20000).get
	}
}

