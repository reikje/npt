package me.rschatz

import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import java.io.File
import scala.util.matching.Regex
import java.lang.Boolean._

/**
 * A SBT plugin to initialize new projects based on existing project templates.
 *
 * @author reik.schatz
 */
object Defaults {
    val organization = "default.organization"
    val name = "default-name"
}

case class NptExecutionSettings(baseDirectory: File)

object Npt extends Plugin {
    private val npt = InputKey[Unit]("npt")

    private val sourceDirName = "src"
    private val mainDirName = "main"
    private val testDirName = "test"
    private val scalaDirName = "scala"
    private val javaDirName = "java"
    private val resourcesDirName = "resources"
    private val buildFileName = "build.sbt"

    val nptSettings = Seq(
        npt := {
            val nptArgs: Seq[String] = spaceDelimited("<arg>").parsed
            println(nptArgs)

            implicit val execSettings = NptExecutionSettings(baseDirectory.value)
            createSrcDirectories()
            createBuildSbt(nptArgs)
        }
    )

    private def createSrcDirectories()(implicit es: NptExecutionSettings) = {
        IO.createDirectories(sourceDirs(es.baseDirectory))
    }

    private def createBuildSbt(nptArgs: Seq[String])(implicit es: NptExecutionSettings) = {
        val org   = """org\:(\S+)""".r
        val name  = """name\:(\S+)""".r

        def matchOrDefault[T](default: T)(pf: PartialFunction[String, T]) =
            nptArgs.collect(pf).headOption.getOrElse(default)

        val (orgValue, nameValue) = (
            matchOrDefault(Defaults.organization) {
                case org(inputOrg)   => inputOrg
            },
            matchOrDefault(Defaults.name) {
                case name(inputName)  => inputName
            }
        )

        IO.writeLines(
            new File(es.baseDirectory, buildFileName),
            List(
                "organization := \"%s\"".format(orgValue),
                "",
                "name := \"%s\"".format(nameValue),
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
