package me.rschatz

import org.scalatest._
import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.io.Source
import me.rschatz.utils.DeletingFileVisitor
import java.nio.charset.Charset
import java.util

/**
 * Tests the utility methods in the [[me.rschatz.PluginExecutor]].
 *
 * @author rschatz
 */
class PluginExecutorTest extends FreeSpec with Matchers with BeforeAndAfterEach {

    var baseDirectory: File = null

    override protected def beforeEach() = {
        baseDirectory = new File(new File(System.getProperty("java.io.tmpdir")), System.nanoTime().toString)
        Files.createDirectory(baseDirectory.toPath)
    }

    override protected def afterEach() = {
        if (baseDirectory.exists()) {
            Files.walkFileTree(baseDirectory.toPath, new DeletingFileVisitor)
        }
    }

    "A PluginExecutor" - {
        "when given tmp dir as base directory " - {
            "when given tmp dir as base directory " - {
                "should build a proper source directory tree" in {
                    val executor = new PluginExecutor(null)
                    val dirs = executor.sourceDirs(baseDirectory)
                    dirs should have size 6
                }
            }

            "should create the source directory tree" in {
                val context = NptExecutionContext(baseDirectory)
                val executor = new PluginExecutor(context)
                executor.createSrcDirectories()

                var dirCount = 0
                Files.walkFileTree(baseDirectory.toPath, new SimpleFileVisitor[Path]() {
                    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
                        dirCount += 1
                        FileVisitResult.CONTINUE    
                    }
                })

                dirCount should be (10)
            }

            "should create the build.sbt file" in {
                val context = NptExecutionContext(baseDirectory, args = List("org:foo", "name:bar"))
                val executor = new PluginExecutor(context)
                executor.createBuildSbt()

                val buildFile = new File(baseDirectory, "build.sbt")

                import scala.collection.JavaConverters._
                val lines = Files.readAllLines(buildFile.toPath, Charset.defaultCharset).asScala
                lines should contain ("organization := \"foo\"")
                lines should contain ("name := \"bar\"")
            }

            "should download and extract archives" in {
                val context = NptExecutionContext(baseDirectory)
                val executor = new PluginExecutor(context)

                executor.downloadTemplate(
                    url = "http://search.maven.org/remotecontent?filepath=javax/jms/javax.jms-api/2.0/javax.jms-api-2.0.jar",
                    tempFolder = baseDirectory
                )

                val downloadDir = new File(baseDirectory, Defaults.downloadDirName)
                val javaxDir = new File(downloadDir, "javax")
                Files.exists(javaxDir.toPath) should be (true)

                val metaInfDir = new File(downloadDir, "META-INF")
                Files.exists(metaInfDir.toPath) should be (true)
            }
        }
    }
}
