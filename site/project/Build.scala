import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Event Mapper"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "mysql" % "mysql-connector-java" % "5.1.+",
			"org.scalatest" %% "scalatest" % "2.0.+" % "test",
			"com.drewnoakes" % "metadata-extractor" % "2.6.+",
			"org.imgscalr" % "imgscalr-lib" % "4.2",
			"jp.t2v" % "play20.auth_2.9.1" % "0.3"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
			testOptions in Test := Nil,
			resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
    )

}
