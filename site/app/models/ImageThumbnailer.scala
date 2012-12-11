
package models

import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import org.imgscalr.Scalr
import org.imgscalr.Scalr._

object ImageThumbnailer {

	def createThumbnail(input: File, output: File, dimension: Int) = {
		val original: BufferedImage = ImageIO.read(input)

		val tooTall = original.getWidth < original.getHeight
		
		val square = if (tooTall) {

			val rescaled: BufferedImage = Scalr.resize(original, Mode.FIT_TO_WIDTH, dimension)
			original.flush()

			val surplus = rescaled.getHeight - dimension
			val cropped = Scalr.crop(rescaled, 0, surplus / 2, dimension, dimension)
			rescaled.flush()
			cropped
			
		} else { // too wide

			val rescaled: BufferedImage = Scalr.resize(original, Mode.FIT_TO_HEIGHT, dimension)
			original.flush()

			val surplus = rescaled.getWidth - dimension
			val cropped = Scalr.crop(rescaled, surplus / 2, 0, dimension, dimension)
			rescaled.flush()
			cropped

		}

		ImageIO.write(square, "jpg", output)
		square.flush()
	}

}

