package me.rschatz

import org.scalatest.{Matchers, FreeSpec}
import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

/**
 * Tests the utility methods in the [[me.rschatz.PluginExecutor]].
 *
 * @author rschatz
 */
class PluginExecutorTest extends FreeSpec with Matchers {
    "A PluginExecutor" - {
        "when given tmp dir as base directory " - {
            val tmpDir = new File(System.getProperty("java.io.tmpdir"))
            "should build a proper source directory tree" in {
                val executor = new PluginExecutor(null)
                val dirs = executor.sourceDirs(tmpDir)
                dirs should have size 6
            }

            "should create the source directory tree" in {
                val baseDirectory = new File(tmpDir, this.getClass.getSimpleName)
                val basePath = baseDirectory.toPath
                Files.deleteIfExists(basePath)
                Files.createDirectory(basePath)

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
        }
    }
}
