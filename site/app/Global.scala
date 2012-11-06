
import play.api._


object Global extends GlobalSettings {
	override def onStart(app: Application) {
		models.StorageBackend() // initialize/verify ImageHandler in config
	}

	override def onStop(app: Application) {
		implicit val a = app
		play.api.libs.concurrent.Akka.system.shutdown()
	}

}
