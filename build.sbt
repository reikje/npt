import bintray.Keys._

lazy val commonSettings = Seq(
  version in ThisBuild := "0.2.0",
  organization in ThisBuild := "me.rschatz",
  libraryDependencies in ThisBuild ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.scalatest" %% "scalatest" % "2.2.5" % "test",
    "org.mockito" % "mockito-core" % "1.10.19" % "test"
  )
)

lazy val root = (project in file(".")).
  settings(commonSettings ++ bintrayPublishSettings: _*).
  settings(
    sbtPlugin := true,
    name := "npt",
    description := "Create new project layouts which can be based on templates",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := false,
    repository in bintray := "sbt-plugins",
    bintrayOrganization in bintray := None
  )