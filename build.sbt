
name          := "content-authorisation-proxy"

organization  := "com.gu"

version       := "0.1"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
  Resolver.sonatypeRepo("releases")
)

testOptions in Test += Tests.Argument("-oF")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  val sprayJsonV = "1.3.2"
  Seq(
    "io.spray" %% "spray-routing-shapeless2" % "1.3.3",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-client" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % sprayJsonV,
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "net.kencochrane.raven" % "raven-logback" % "6.0.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.10.50",
    "com.gu" %% "membership-common" % "0.242",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.gu" %% "scanamo" % "0.6.0",
    "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.62"
  )
}

lazy val root = (project in file(".")).enablePlugins(
  BuildInfoPlugin,
  RiffRaffArtifact,
  SbtNativePackager,
  JavaAppPackaging
).settings(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
      "git rev-parse HEAD".!!.trim
    } catch { case e: Exception => "unknown" })),
    BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
    BuildInfoKey.constant("buildTime", System.currentTimeMillis)
  ),
  buildInfoOptions += BuildInfoOption.ToMap,
  buildInfoPackage := "app"
)

mappings in Universal ++= NativePackagerHelper.contentOf("cloudformation/resources")

riffRaffPackageType := (packageBin in config("universal")).value

addCommandAlias("devrun", "re-start --- -Dconfig.resource=DEV.conf")

Revolver.settings
