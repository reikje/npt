package me.rschatz

import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import java.io.File
import scala.util.matching.Regex
import java.lang.Boolean._
import java.net.URL
import scala.util.{Success, Failure, Try}

/**
 * A SBT plugin to initialize new projects based on existing project templates.
 *
 * @author reik.schatz
 */
object Defaults {
    val organization = "default.organization"
    val name = "default-name"
}

class NptExecutionContext(val baseDirectory: File, val args: Seq[String], val log: sbt.Logger) {
    def inputArgs() = {
        val Org   = """org\:(\S+)""".r
        val Name  = """name\:(\S+)""".r

        val orgValue = args.collect({
            case Org(inputOrg) => Some(inputOrg) }
        ).headOption.getOrElse(Some(Defaults.organization))

        val nameValue = args.collect({
            case Name(inputName) => Some(inputName) }
        ).headOption.getOrElse(Some(Defaults.name))

        val filtered = args.filter({
            case Org(inputOrg) => false
            case Name(inputName) => false
            case _ => true
        })

        val templateValue = filtered.map(s => Some(s)).headOption.getOrElse(None)

        (orgValue, nameValue, templateValue)
    }
}

object Npt extends Plugin {
    private val npt = InputKey[Unit]("npt")

    private val nptCopy = InputKey[Unit]("npy-copy")

    private val DownloadableArchive = "^(https?|ftp|file)://.+\\.(gz|zip|jar)$".r

    private val sourceDirName = "src"
    private val mainDirName = "main"
    private val testDirName = "test"
    private val scalaDirName = "scala"
    private val javaDirName = "java"
    private val resourcesDirName = "resources"
    private val buildFileName = "build.sbt"

    private val defaultTemplate = "SBT.NPT.DEFAULT.TEMPLATE"
    private val templateFolder = "SBT.NPT.TEMPLATE.FOLDER"

    val nptSettings = Seq(
        npt := {
            val nptArgs: Seq[String] = spaceDelimited("<arg>").parsed
            println(nptArgs)

            implicit val execContext = new NptExecutionContext(baseDirectory.value, nptArgs, streams.value.log)
            createSrcDirectories()
            createBuildSbt()
            copyTemplate()
        },
        nptCopy := {
            val nptArgs: Seq[String] = spaceDelimited("<arg>").parsed
            implicit val execContext = new NptExecutionContext(baseDirectory.value, nptArgs, streams.value.log)
            copyTemplate()
        }
    )

    private def copyTemplate()(implicit es: NptExecutionContext): Unit = {
        val log = es.log
        log.info(s"Finding template to copy")

        var folderToCopy: Option[File] = None
        val defaultTemplate = fromDefaultTemplate()
        if (defaultTemplate.isDefined) {
            folderToCopy = defaultTemplate
        } else {
            val defaultFolder = fromDefaultFolder()
            if (defaultFolder.isDefined) {
                folderToCopy = defaultFolder
            } else {
                val fromInput = fromInputArgs()
                if (fromInput.isDefined) {
                    folderToCopy = fromInput
                }
            }
        }
        
        if (folderToCopy.isDefined) {
            IO.copyDirectory(folderToCopy.get, es.baseDirectory)
        } else {
            log.info(s"Not copying a template")
        }
    }
    
    private def fromInputArgs()(implicit es: NptExecutionContext): Option[File] = {
        val log = es.log

        val (_, _, templateNameOption) = es.inputArgs()
        if (templateNameOption.isDefined) {
            val templateName = templateNameOption.get
            log.info(s"Trying template name or location from input ($templateName)")

            templateName match {
                case DownloadableArchive(protocol, extension) => downloadTemplate(templateName)
                case _ => templateFolder(new File(templateName))
            }
        } else {
            None
        }
    }
    

    private def fromDefaultTemplate()(implicit es: NptExecutionContext): Option[File] = {
        // check SBT.NPT.DEFAULT.TEMPLATE
        val log = es.log

        for (props <- List(sys.env, sys.props)) {
            val defaultTemplateName = props.get(defaultTemplate)
            if (defaultTemplateName.isDefined) {
                val defaultTemplateNameValue = defaultTemplateName.get
                log.info(s"Trying $defaultTemplate ($defaultTemplateNameValue)")
                return downloadTemplate(defaultTemplateNameValue)
            }
        }
        None
    }

    private def fromDefaultFolder()(implicit es: NptExecutionContext): Option[File] = {
        // check SBT.NPT.TEMPLATE.FOLDER
        // if SBT.NPT.TEMPLATE.FOLDER => get template name from args and copy this template
        val log = es.log

        for (props <- List(sys.env, sys.props)) {
            val templateFolderName = props.get(templateFolder)
            if (templateFolderName.isDefined) {
                val templateFolderNameValue = templateFolderName.get
                log.info(s"Trying $templateFolder ($templateFolderNameValue)")
                val folder = new File(templateFolderNameValue)
                if (folder.exists()) {
                    val (_, _, templateNameOption) = es.inputArgs()
                    if (templateNameOption.isDefined) {
                        val templateName = templateNameOption.get
                        return templateFolder(new File(folder, templateName))
                    } else {
                        log.info(s"Missing name of a template in folder $templateFolderNameValue")
                    }
                } else {
                    log.info(s"Folder: $templateFolderName is not an existing folder")
                }
            }
        }
        None
    }

    private def templateFolder(directory: File)(implicit es: NptExecutionContext): Option[File] = {
        val log = es.log
        if (directory.isDirectory) {
            log.info(s"Existing folder $directory")
            Some(directory)
        } else {
            log.info(s"$directory does not exist or is not a directory. Skipping")
            None
        }
    }

    private def downloadTemplate(url: String)(implicit es: NptExecutionContext): Option[File] = {
        val log = es.log

        def download(url: String): Try[File] = {
            url match {
                case DownloadableArchive(protocol, extension) =>
                    val workFolder = "NPT-DOWNLOAD"
                    val targetFolder = new File(IO.temporaryDirectory, workFolder)
                    if (targetFolder.exists()) {
                        log.info(s"Deleting pre-existing temporary folder")
                        IO.delete(targetFolder)
                    }

                    log.info(s"Creating temporary folder ($targetFolder)")
                    IO.createDirectory(targetFolder)

                    log.info(s"Unzipping to temporary folder")

                    try {
                        val templateURL = new URL(url)
                        val files = IO.unzipURL(templateURL, targetFolder).toList
                        files match {
                            case Nil => Failure(new IllegalStateException(s"Archive could not be downloaded or contains no files"))
                            case _ => Success(targetFolder)
                        }
                    } catch {
                        case e: Throwable => Failure(e)
                    }
                case _ => Failure(new IllegalArgumentException(s"URL: $url does not denote a downloadable archive"))
            }
        }

        download(url) match {
            case Success(f) => Some(f)
            case Failure(e) =>
                log.info(e.getMessage)
                None
        }
    }

    private def createSrcDirectories()(implicit es: NptExecutionContext) = {
        val log = es.log
        log.info("Creating source folders")

        IO.createDirectories(sourceDirs(es.baseDirectory))
    }

    private def createBuildSbt()(implicit es: NptExecutionContext) = {
        val log = es.log
        log.info("Creating build.sbt")

        val (orgValueOption, nameValueOption, _) = es.inputArgs()
        IO.writeLines(
            new File(es.baseDirectory, buildFileName),
            List(
                "organization := \"%s\"".format(orgValueOption.get),
                "",
                "name := \"%s\"".format(nameValueOption.get),
                "",
                "version := \"0.1-SNAPSHOT\"",
                "",
                "scalaVersion := \"2.10.4\""
            )
        )
    }

    private def sourceDirs(baseDirectory: File) = {
        Seq(sourceDirName).flatMap({
            rootDir => Seq(mainDirName, testDirName).flatMap({
                subRootDir => Seq(scalaDirName, javaDirName, resourcesDirName).map({
                    dirName => new File(new File(new File(new File(baseDirectory.toString), rootDir), subRootDir), dirName)
                })
            })
        })
    }
}
