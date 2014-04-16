import bintray.Keys._

sbtPlugin := true

organization := "me.rschatz"

name := "npt"

version := "0.1.0"

scalaVersion := "2.10.4"

publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

// This is an example.  bintray-sbt requires licenses to be specified
// (using a canonical name).
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.0" % "test"

libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5" % "test"