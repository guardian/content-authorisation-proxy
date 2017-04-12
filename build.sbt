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
    "com.gu" %% "membership-common" % "0.386",
    "com.gu" %% "content-authorisation-common" % "0.1",
    "com.gu" %% "scanamo" % "0.9.2",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )
}

lazy val root = (project in file(".")).enablePlugins(
  BuildInfoPlugin,
  RiffRaffArtifact,
  JDebPackaging,
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

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd
debianPackageDependencies := Seq("openjdk-8-jre-headless")
javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null",
      "-J-XX:MaxRAMFraction=2",
      "-J-XX:InitialRAMFraction=2",
      "-J-XX:MaxMetaspaceSize=500m",
      "-J-XX:+PrintGCDetails",
      "-J-XX:+PrintGCDateStamps",
      "-Dscala.concurrent.context.maxThreads=64",
      "-Dscala.concurrent.context.numThreads=64",
      s"-J-Xloggc:/var/log/${name.value}/gc.log"
    )

maintainer := "Membership Dev <membership.dev@theguardian.com>"
packageSummary := "Content Authorization Proxy"
packageDescription := """Content Authorization Proxy"""
riffRaffPackageType := (packageBin in Debian).value

addCommandAlias("devrun", "re-start")

Revolver.settings
