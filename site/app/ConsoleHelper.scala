
import play.api.Play
import play.api.Application
import java.io.File
import play.api.Mode._

object ConsoleHelpers {

	implicit def app = Play.current

	def startDevApp {
		val app = new Application(new File("."), getClass.getClassLoader, None, Dev)
		Play.start(app)
	}

	def startProdApp {
		val app = new Application(new File("."), getClass.getClassLoader, None, Prod)
		Play.start(app)
	}

}
