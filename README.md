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

Now you should globally mix in `nptSettings`. Create a file under `~/.sbt/0.13` called `npt.sbt` containing the line

    seq(me.rschatz.Npt.nptSettings: _*)
	
## Usage

There are several ways to use the npt plug-in.

### Without specifying a template project to copy

This way, the plug-in behaves almost exactly like the np plug-in. It will create the `src` folder and subfolders and also a `build.sbt` file for you but nothing else.

    $ mkdir newproject
	$ cd newproject
    $ sbt npt
    $ [info] Creating source folders
    $ [info] Creating build.sbt
    $ [info] Finding template to copy
    $ [info] Not copying a template

### Directly specifying a template project to copy

It is possible to specify a downloadable archive (zip, gz or jar) or to point to a directory on the local file system. If invoking `npt` with a URL denoting an archive that 
can be downloaded, the plug-in will download the file to a temporary location, extract the file and copy it's content into your new project.

    $ mkdir newproject
	$ cd newproject
    $ sbt npt http://our.company.nexus:8081/nexus/content/repositories/releases/me/company/sbt-project-templates/1.0/mvc.zip
    $ [info] Creating source folders
    $ [info] Creating build.sbt
    $ [info] Finding template to copy
    $ [info] Trying template name or location from input (http://our.company.nexus:8081/nexus/content/repositories/releases/me/company/sbt-project-templates/1.0/mvc.zip)
    $ [info] Deleting pre-existing temporary folder
    $ [info] Creating temporary folder (/tmp/NPT-DOWNLOAD)
    $ [info] Unzipping to temporary folder