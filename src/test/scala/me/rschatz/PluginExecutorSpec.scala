package me.rschatz

import java.io.File
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.scalatest._

/**
 * Tests the utility methods in the [[me.rschatz.PluginExecutor]].
 *
 * @author rschatz
 */
class PluginExecutorSpec extends FreeSpec with Matchers with Sandbox {

  "A PluginExecutor" - {
    "when given tmp dir as base directory " - {
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

        dirCount shouldBe 10
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
    }
  }
}
