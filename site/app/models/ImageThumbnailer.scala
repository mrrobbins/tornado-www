
package models

import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import org.jdesktop.swingx.graphics.GraphicsUtilities

object ImageThumbnailer {
	val path = "/tmp/pending/thumbnails/"
	val img_width = 600;

	def createThumbnail(file: File, filename: String) = {
		val outputPath = path + filename
		val originalImage: BufferedImage = ImageIO.read(file)
		try {
			val thumbnailFile: BufferedImage = GraphicsUtilities.createThumbnail(originalImage, img_width)
			ImageIO.write(thumbnailFile, "jpg", new File(outputPath))
		} catch {
			case _: IllegalArgumentException => 
				ImageIO.write(originalImage, "jpg", new File(outputPath))
		}
	}

	def lookup(key: String): String = {
		"/image/thumb/" + key
	}
}

