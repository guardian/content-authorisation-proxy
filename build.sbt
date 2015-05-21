name          := "content-authorisation-proxy"

organization  := "com.gu"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases"

testOptions in Test += Tests.Argument("-oF")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-client"  % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-slf4j"    % akkaV,
    "ch.qos.logback"      %   "logback-classic" % "1.1.2")
}

lazy val root = (project in file("."))
  .enablePlugins(RiffRaffArtifact)
  .enablePlugins(SbtNativePackager)
  .enablePlugins(JavaAppPackaging)

mappings in Universal ++= NativePackagerHelper.contentOf("cloudformation/resources")

riffRaffPackageType := (packageBin in config("universal")).value

addCommandAlias("devrun", "re-start --- -Dconfig.resource=DEV.conf")

Revolver.settings
