name := "xmlsoap-ersatz"

organization := "play2.tools.xml"

version := "0.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "junit" % "junit" % "4.8" % "test"  
)

publishTo <<=  version { (v: String) => 
    val base = "../../workspace_mandubian/mandubian-mvn"
	if (v.trim.endsWith("SNAPSHOT")) 
		Some(Resolver.file("snapshots", new File(base + "/snapshots")))
	else Some(Resolver.file("releases", new File(base + "/releases")))
}

publishMavenStyle := true

publishArtifact in Test := false

