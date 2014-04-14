# npt

A SBT plug-in to create new projects layouts based on existing project templates. 

## Motivation

The np-plugin is great to initialize new SBT projects. This plug-in goes a bit further because you can copy a pre-existing templates on top of your project structure. So let's say your default project layout looks like this:

    |
    |- src
	|	- main
	|		- scala
	|			- com
	|				- company
	|					- Main.scala
	|		- resources
	|		- java
	|	- test
	|		- scala
	|			- com
	|				- company
	|					- MainTest.scala
	|		- resources
	|			- db_schema.sql
	|		- java
	|- deploy
	|	- fabfile.py
	|build.sbt
	
And your default build.sbt is this:

    organization := "com.company"

    name := "<project_name>"

    version := "1.0-SNAPSHOT"

    scalaVersion := "2.10.4"
	
	// resolvers
    resolvers ++= Seq(
        "Sonatype (Snapshots)" at "http://oss.sonatype.org/content/repositories/snapshots",
        "Sonatype (Releases)" at "http://oss.sonatype.org/content/repositories/releases"
    )

    // publishing
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

	publishTo := {
        val nexus = "http://our.company.nexus:8081/"
        if (version.value.trim.endsWith("SNAPSHOT")) {
            Some("Snapshots" at nexus + "nexus/content/repositories/snapshots")
        } else {
            Some("Releases" at nexus + "nexus/content/repositories/releases")
        }
    }
	
	// dependencies
	libraryDependencies ++= Seq(
	    "com.company" %% "our.cool.utility-lib" % "1.0",
		"org.scalatest" %% "scalatest" % "2.1.3" % "test"
	}
	
You could always create a default SBT project and manually tweak everything, but that would be tedious. This plug-in aims to make it simpler for you.

## Installation

Often you want to install this as a global plug-in.

### For sbt 0.13+

If you don't already have one, create an `~/.sbt/0.13/plugins` directory. And inside of it, create a file `npt.sbt` containing the line

    addSbtPlugin("me.rschatz" % "npt" % "0.1")
    
This will make `nptSettings` globally visible to your project definitions.

If you wish to globally mix in `nptSettings`, create a file under `~/.sbt/0.13` called `npt.sbt` containing the line

    seq(me.rschatz.Npt.nptSettings: _*)