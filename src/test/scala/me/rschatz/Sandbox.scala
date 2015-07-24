package me.rschatz

import java.io.File
import java.nio.file.Files

import me.rschatz.utils.DeletingFileVisitor
import org.scalatest.{Suite, BeforeAndAfterEach}

trait Sandbox extends BeforeAndAfterEach { this: Suite =>
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
}
