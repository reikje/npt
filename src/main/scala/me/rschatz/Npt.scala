package me.rschatz

import java.io.File
import java.net.URL

import me.rschatz.Defaults._
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._

import scala.util.{Failure, Success, Try}

/**
 * A SBT plugin to initialize new projects based on existing project templates.
 *
 * @author reik.schatz
 */
object Defaults {
  val organization = "default.organization"
  val name = "default-name"

  val DownloadableArchive = "^(https?|ftp|file)://.+\\.(gz|zip|jar)$".r

  val sourceDirName = "src"
  val mainDirName = "main"
  val testDirName = "test"
  val scalaDirName = "scala"
  val javaDirName = "java"
  val resourcesDirName = "resources"
  val downloadDirName = "nptdownloads"
  val buildFileName = "build.sbt"

  val defaultTemplateProperty = "SBT_NPT_DEFAULT_TEMPLATE"
  val templateFolderProperty = "SBT_NPT_TEMPLATE_FOLDER"

  implicit class RichOptionCompanion(val self: Option.type) extends AnyVal {
    def when[A](cond: Boolean)(value: => A): Option[A] = if(cond) Some(value) else None
  }

  implicit class IfNoneThenAction[A](val self: Option[A]) extends AnyVal {
    def ifNone(action: => Unit) = { if (self.isEmpty) action; self }
  }
}

/**
 * Another logging abstraction.
 */
trait NptLogger {
  def debug(msg: String) = {}
  def info(msg: String) = {}
  def warn(msg: String) = {}
}

/**
 * Wraps the SBT logger.
 * @param log a [[sbt.Logger]]
 */
class WrappedSBTLogger(log: sbt.Logger) extends NptLogger {
  override def debug(msg: String) = log.debug(msg)
  override def info(msg: String) = log.info(msg)
  override def warn(msg: String) = log.warn(msg)
}

object NptLogger {
  object Blackhole extends NptLogger

  /**
   * Returns a [[NptLogger]] that doesn't log anything.
   * @return [[NptLogger]]
   */
  def empty = Blackhole
}

/**
 * Wraps the context in which the plugin is executing.
 *
 * @param baseDirectory root directory
 * @param args additional input arguments
 * @param log a [[NptLogger]]
 * @param tempFolder folder for temporary downloads
 */
case class NptExecutionContext(baseDirectory: File, args: Seq[String] = Nil, log: NptLogger = NptLogger.empty,
                               tempFolder: File = IO.temporaryDirectory) {

  def inputArgs() = {
    val Org = """org\:(\S+)""".r
    val Name = """name\:(\S+)""".r

    val orgValue = args.collect({
      case Org(inputOrg) => Some(inputOrg)
    }
    ).headOption.getOrElse(Some(Defaults.organization))

    val nameValue = args.collect({
      case Name(inputName) => Some(inputName)
    }
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

abstract class FolderFinder(log: NptLogger = NptLogger.empty) {
  protected val fileActions: FileActions = new FileActions(log)

  def get: Option[File]
}

/**
 * A [[FolderFinder]] that returns potential template [[java.io.File]]s based on input arguments to the '''npt''' task.
 * Accepted input args are:
 *
 *   1. a downloadable archive
 *   2. the path to a folder on the local file system
 *
 * @return Option[File]
 */
class FromInputArgs(templateName: Option[String], tempFolder: File = IO.temporaryDirectory, log: NptLogger = NptLogger.empty) extends FolderFinder(log) {
  override def get: Option[sbt.File] = {
    templateName.flatMap {
      t =>
        log.info(s"Trying template name or location from input ($t)")

        t match {
          case DownloadableArchive(protocol, extension) => fileActions.downloadTemplate(t, tempFolder)
          case _ => fileActions.templateFolder(new File(t))
        }
    }
  }
}

/**
 * A [[FolderFinder]] which potentially returns a [[java.io.File]] if the environment or system property
 * '''SBT_NPT_DEFAULT_TEMPLATE''' is set.
 *
 * @return Option[File]
 */
class FromTemplateProperty(tempFolder: File = IO.temporaryDirectory, log: NptLogger = NptLogger.empty) extends FolderFinder(log) {
  override def get: Option[sbt.File] = {
    import Defaults._

    val propertySources = Seq(sys.env, sys.props)

    propertySources.toIterator.flatMap {
      props =>
        for {
          defaultTemplate <- props.get(defaultTemplateProperty)
        } yield {
          log.info(s"Trying $defaultTemplateProperty ($defaultTemplate)")
          fileActions.downloadTemplate(defaultTemplate, tempFolder) orElse fileActions.templateFolder(new File(defaultTemplate))
        }
    }.find(_.isDefined).flatten
  }
}

/**
 * A [[FolderFinder]] which potentially returns a [[java.io.File]] if the environment or system property
 * '''SBT_NPT_TEMPLATE_FOLDER''' is set and the name of a template sub-folder is given.
 *
 * @return Option[File]
 */
class FromFolderProperty(templateNameOption: Option[String], log: NptLogger = NptLogger.empty) extends FolderFinder(log) {
  override def get: Option[sbt.File] = {
    import Defaults._

    val propertySources = Seq(sys.env, sys.props)
    propertySources.toIterator.flatMap {
      props =>
        for {
          folderName <- props.get(templateFolderProperty)
        } yield {
          log.info(s"Trying $templateFolderProperty ($folderName)")
          val folder = new File(folderName)
          Option.when(folder.exists()) {
            templateNameOption.ifNone(s"Missing name of a template in folder $folderName").flatMap {
              templateName => fileActions.templateFolder(new File(folder, templateName))
            }
          }.ifNone(s"Folder $folderName is not an existing folder").flatten
        }
    }.find(_.isDefined).flatten
  }
}

/**
 * Wraps a bunch of useful file functions.
 * @param log a [[NptLogger]]
 */
class FileActions(log: NptLogger = NptLogger.empty) {

  /**
   * Downloads and extracts the archive denoted by the given '''url''' and returns the directory ([[File]]) that
   * contains the extracted content. Returns '''None''' if the '''url''' is not a downloadable archive, or the file
   * cannot be downloaded.
   *
   * @param url a URL to a archive file
   * @param tempFolder temporary download folder
   * @return Optio[File]
   */
  def downloadTemplate(url: String, tempFolder: File = IO.temporaryDirectory): Option[File] = {
    def download(url: String): Try[File] = {
      url match {
        case DownloadableArchive(protocol, extension) =>
          val targetFolder = new File(tempFolder, downloadDirName)
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

  /**
   * Wraps the given [[File]] into an [[Option]] that will be defined if it is a directory.
   * @param directory any File
   * @return Option[File]
   */
  def templateFolder(directory: File): Option[File] = {
    if (directory.isDirectory) {
      log.info(s"Existing folder $directory")
      Some(directory)
    } else {
      log.info(s"$directory does not exist or is not a directory. Skipping")
      None
    }
  }

  /**
   * Returns a sequence of files representing the default directory layout.
   * @param baseDirectory the root folder
   * @return Seq[File]
   */
  def defaultLayout(baseDirectory: File): Seq[File] = {
    Seq(sourceDirName).flatMap({
      rootDir => Seq(mainDirName, testDirName).flatMap({
        subRootDir => Seq(scalaDirName, javaDirName, resourcesDirName).map({
          dirName => new File(new File(new File(new File(baseDirectory.toString), rootDir), subRootDir), dirName)
        })
      })
    })
  }
}

/**
 * Contains all steps that the [[sbt.AutoPlugin]] executes.
 *
 * @param es the context of execution
 */
class PluginExecutor(val es: NptExecutionContext) {
  private val (_, _, templateName) = es.inputArgs()
  private val finders: Seq[FolderFinder] = List(
    new FromTemplateProperty(log = es.log),
    new FromFolderProperty(templateName, log = es.log),
    new FromInputArgs(templateName, log = es.log)
  )

  /**
   * Tries to find a template using different lookup methods. If a template is found, it is copyied into the root
   * folder.
   */
  def copyTemplate(): Unit = {
    val log = es.log
    log.info(s"Finding template to copy")

    val folderToCopy = finders.toIterator.map(_.get).find(_.isDefined).flatten
    folderToCopy.ifNone {
      log.info(s"Not copying a template")
    }.foreach {
      f => IO.copyDirectory(f, es.baseDirectory)
    }
  }

  /**
   * Creates the default project layout inside the root folder.
   */
  def createSrcDirectories(): Unit = {
    val log = es.log
    log.info("Creating source folders")

    val fileActions = new FileActions()
    IO.createDirectories(fileActions.defaultLayout(es.baseDirectory))
  }

  /**
   * Creates the default '''build.sbt''' file.
   */
  def createBuildSbt(): Unit = {
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
        "scalaVersion := \"2.10.5\""
      )
    )
  }
}

object NptPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val npt = InputKey[Unit](
      "npt",
      "creates the project layout"
    )
  }

  import autoImport._

  override def projectSettings = Seq(
    npt := {
      val nptArgs: Seq[String] = spaceDelimited("<arg>").parsed

      val context = NptExecutionContext(baseDirectory.value, nptArgs, new WrappedSBTLogger(streams.value.log))
      val executor = new PluginExecutor(context)
      executor.createSrcDirectories()
      executor.createBuildSbt()
      executor.copyTemplate()
    }
  )
}
