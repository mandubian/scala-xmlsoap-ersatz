import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "play2-xmlsoap"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "play2.tools.xml" %% "xmlsoap-ersatz" % "0.1-SNAPSHOT"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += ("mandubian-mvn snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots")
    )

}
