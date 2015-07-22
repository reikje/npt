# npt

A SBT plug-in to create new projects layouts based on existing project templates. 

## Motivation

The [np plug-in](https://github.com/softprops/np) is a great way to initialize new SBT projects. This plug-in also let's you initialize project skeletons for new projects that are build with SBT. On top, it allows you to copy a pre-existing template into your new project structure. So let's say whenever you create a new project, you want the project layout to look like this:

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
	
For your default build.sbt you want it to contain the following rows:

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
	
Of course you could always create a default SBT project and manually tweak everything - but that would be tedious. This plug-in aims to make it simpler for you.

## Installation

Often you want to install this as a global plug-in.

### For sbt 0.13.5+

If you don't already have one, create an `~/.sbt/0.13/plugins` directory. And inside of it, create a file `npt.sbt` containing the line

    addSbtPlugin("me.rschatz" % "npt" % "0.2.0")
    
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
	
If invoking `npt` with a path denoting a directory on the local file system, the plug-in will copy it's content into your new project.

    $ mkdir newproject
	$ cd newproject
    $ sbt npt D:\test
    $ [info] Creating source folders
    $ [info] Creating build.sbt
    $ [info] Finding template to copy
    $ [info] Trying template name or location from input (D:\test)
    $ [info] Existing folder D:\test
	
### Default template system property or environment variable

A default template can be specified as a system property (when running SBT) or as a environment variable. The name is `SBT_NPT_DEFAULT_TEMPLATE` and the value has to be a URL denoting an archive (zip, gz or jar) that can be downloaded OR a path denoting an existing directory on the local file system. If you have SBT_NPT_DEFAULT_TEMPLATE set you don't have to specify an additional argument when invoking `npt`.

    $ mkdir newproject
	$ cd newproject
    $ sbt -DSBT_NPT_DEFAULT_TEMPLATE=D:\\test
	$ npt
    $ [info] Creating source folders
    $ [info] Creating build.sbt
    $ [info] Finding template to copy
    $ [info] Trying SBT_NPT_DEFAULT_TEMPLATE (D:\test)
    $ [info] URL: D:\test does not denote a downloadable archive
    $ [info] Existing folder D:\test
	
### Default folder system property or environment variable

Often you have a bunch of project templates to choose from, i.e. a api project template, a web project template and so on. For easy access to these project templates you can use the `SBT_NPT_TEMPLATE_FOLDER` system property or a environment variable of the same name. `SBT_NPT_TEMPLATE_FOLDER` must be an existing folder on the local file system. After specifying `SBT_NPT_TEMPLATE_FOLDER` you execute `npt` and specify the name of a sub-folder inside `SBT_NPT_TEMPLATE_FOLDER`. Let's say you have created the following folders.

    |- /opt/sbt/templates 
					|- api					
					|- web
					|- service

Then you would create a new `api` project like this.
					
    $ mkdir newproject
	$ cd newproject
    $ export SBT_NPT_TEMPLATE_FOLDER=/opt/sbt/templates
	$ npt api
	$ [info] Creating source folders
	$ [info] Creating build.sbt
	$ [info] Finding template to copy
	$ [info] Trying SBT_NPT_TEMPLATE_FOLDER (/opt/sbt/templates)
	$ [info] Existing folder /opt/sbt/templates/api
	
## Customization

Similar to the [np plug-in](https://github.com/softprops/np) you can customize some values in the build.sbt file that gets generated. For full customization you can always have your own build.sbt file in the project template that you are applying, which will overwrite the generated build.sbt file. The following values can be customized in the default generated build.sbt file.

| Setting       | Default              | Customization      |
| ------------- |----------------------|--------------------|
| name          | default.organization | npt name:newapp    |
| organization  | default-name         | npt org:me.company |