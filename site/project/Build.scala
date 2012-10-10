import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Event Mapper"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "mysql" % "mysql-connector-java" % "5.1.21",
			"org.scalatest" %% "scalatest" % "2.0.M4" % "test"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
			testOptions in Test := Nil
    )

}
