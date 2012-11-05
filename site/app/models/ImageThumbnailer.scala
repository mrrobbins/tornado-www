
package models

import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import org.imgscalr.Scalr
import org.imgscalr.Scalr._

object ImageThumbnailer {
	val path = "/tmp/pending/thumbnails/"
	val img_width = 600;

	def createThumbnail(file: File, filename: String) = {
		val outputPath = path + filename
		val original: BufferedImage = ImageIO.read(file)

		val tooTall = original.getWidth < original.getHeight
		
		val square = if (tooTall) {

			val rescaled: BufferedImage = Scalr.resize(original, Mode.FIT_TO_WIDTH, 600)
			original.flush()

			val surplus = rescaled.getHeight - 600
			val cropped = Scalr.crop(rescaled, 0, surplus / 2, 600, 600)
			rescaled.flush()
			cropped
			
		} else { // too wide

			val rescaled: BufferedImage = Scalr.resize(original, Mode.FIT_TO_HEIGHT, 600)
			original.flush()

			val surplus = rescaled.getWidth - 600
			val cropped = Scalr.crop(rescaled, surplus / 2, 0, 600, 600)
			rescaled.flush()
			cropped

		}

		ImageIO.write(square, "jpg", new File(outputPath))
		square.flush()
	}

	def lookup(key: String): String = {
		"/image/thumb/" + key
	}
}

