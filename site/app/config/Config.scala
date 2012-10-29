package config

import play.api._
import models._

object Config {
	val imageHandler: ImageHandler = new LocalImageHandler
}

