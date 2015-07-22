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

        dirCount should be(10)
      }

      "should create the build.sbt file" in {
        val context = NptExecutionContext(baseDirectory, args = List("org:foo", "name:bar"))
        val executor = new PluginExecutor(context)
        executor.createBuildSbt()

        val buildFile = new File(baseDirectory, "build.sbt")

        import scala.collection.JavaConverters._
        val lines = Files.readAllLines(buildFile.toPath, Charset.defaultCharset).asScala
        lines should contain("organization := \"foo\"")
        lines should contain("name := \"bar\"")
      }

      "should download and extract archives" in {
        val context = NptExecutionContext(baseDirectory = baseDirectory, tempFolder = baseDirectory)
        val executor = new PluginExecutor(context)

        executor.downloadTemplate(
          "https://github.com/reikje/npt/archive/master.zip"
        )

        val downloadDir = new File(baseDirectory, Defaults.downloadDirName)
        val masterDir = new File(downloadDir, "npt-master")
        Files.exists(masterDir.toPath) shouldBe true
      }

      "should figure out if a directory exists from templateFolder function" in {
        val context = NptExecutionContext(baseDirectory)
        val executor = new PluginExecutor(context)
        executor.templateFolder(baseDirectory) should be(Some(baseDirectory))
        executor.templateFolder(new File(baseDirectory, "FOO")) should be(None)
      }

      "should figure out the template folder based on default folder property" in {
        val subDir = new File(baseDirectory, "DEFAULT_FOLDER")
        Files.createDirectory(subDir.toPath)

        var context = NptExecutionContext(baseDirectory = baseDirectory, args = List("DEFAULT_FOLDER"))
        var executor = new PluginExecutor(context)

        sys.props += Defaults.templateFolderProperty -> baseDirectory.getPath

        executor.fromDefaultFolder() should be(Some(subDir))

        context = NptExecutionContext(baseDirectory = baseDirectory, args = List("NON_EXISTING"))
        executor = new PluginExecutor(context)
        executor.fromDefaultFolder() should be(None)

        sys.props -= Defaults.templateFolderProperty
      }

      "should figure out the template folder based on default template property" in {
        val context = NptExecutionContext(baseDirectory = baseDirectory, tempFolder = baseDirectory)
        val executor = new PluginExecutor(context)

        executor.fromDefaultTemplate() shouldBe None

        sys.props.update(Defaults.defaultTemplateProperty, "https://github.com/reikje/npt/archive/master.zip")
        executor.fromDefaultTemplate() shouldBe Some(new File(baseDirectory, Defaults.downloadDirName))

        sys.props.update(Defaults.defaultTemplateProperty, baseDirectory.getPath)
        executor.fromDefaultTemplate() shouldBe Some(baseDirectory)

        sys.props.update(Defaults.defaultTemplateProperty, "https://github.com/reikje/npt/NON-EXISTENT/master.zip")
        executor.fromDefaultTemplate() shouldBe None

        sys.props.update(Defaults.defaultTemplateProperty, new File(baseDirectory, "NON-EXISTING").getPath)
        executor.fromDefaultTemplate() shouldBe None
      }

      "should figure out the template folder based on input args" in {
        var executor = new PluginExecutor(
          NptExecutionContext(
            baseDirectory = baseDirectory,
            tempFolder = baseDirectory,
            args = List("https://github.com/reikje/npt/archive/master.zip")
          )
        )
        executor.fromInputArgs() shouldBe Some(new File(baseDirectory, Defaults.downloadDirName))

        executor = new PluginExecutor(
          NptExecutionContext(
            baseDirectory = baseDirectory,
            args = List(baseDirectory.getPath)
          )
        )
        executor.fromInputArgs() shouldBe Some(baseDirectory)

        executor = new PluginExecutor(
          NptExecutionContext(
            baseDirectory = baseDirectory,
            args = List(new File(baseDirectory, "NON_EXISTING").getPath)
          )
        )
        executor.fromInputArgs() shouldBe None

        executor = new PluginExecutor(
          NptExecutionContext(
            baseDirectory = baseDirectory,
            tempFolder = baseDirectory,
            args = List("https://github.com/reikje/npt/NON-EXISTENT/master.zip")
          )
        )
        executor.fromInputArgs() shouldBe None
      }
    }
  }
}
