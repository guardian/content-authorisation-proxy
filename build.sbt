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
    "ch.qos.logback"      %   "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.9.24"
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
  buildInfoPackage := "app"
)

mappings in Universal ++= NativePackagerHelper.contentOf("cloudformation/resources")

riffRaffPackageType := (packageBin in config("universal")).value

addCommandAlias("devrun", "re-start --- -Dconfig.resource=DEV.conf")

Revolver.settings
