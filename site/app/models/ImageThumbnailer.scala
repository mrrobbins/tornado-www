
package models

import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import org.jdesktop.swingx.graphics.GraphicsUtilities

object ImageThumbnailer {
	val path = "/tmp/pending/thumbs/"
	val img_width = 600;

	def createThumbnail(file: File, filename: String) = {
		val outputPath = path + filename
		val originalImage: BufferedImage = ImageIO.read(file)
		val thumbnailFile: BufferedImage = GraphicsUtilities.createThumbnail(originalImage, img_width)
		ImageIO.write(thumbnailFile, "jpg", new File(outputPath))
	}

	def lookup(key: String): String = {
		"/image/" + key
	}
}

