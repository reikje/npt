package me.rschatz

import java.io.File
import java.nio.file.Files

import org.scalatest.{Matchers, WordSpec}

class FileActionsSpec extends WordSpec with Matchers with Sandbox {
  "FileActions" should {
    "download and extract archives" in {
      val fileActions = new FileActions()

      fileActions.downloadTemplate(
        "https://github.com/reikje/npt/archive/master.zip", tempFolder = baseDirectory
      )

      val downloadDir = new File(baseDirectory, Defaults.downloadDirName)
      val masterDir = new File(downloadDir, "npt-master")
      Files.exists(masterDir.toPath) shouldBe true
    }

    "build a proper source directory tree" in {
      val fileActions = new FileActions()
      val dirs = fileActions.sourceDirs(baseDirectory)
      dirs should have size 6
    }

    "figure out if a directory exists from templateFolder function" in {
      val fileActions = new FileActions()
      fileActions.templateFolder(baseDirectory) shouldBe Some(baseDirectory)
      fileActions.templateFolder(new File(baseDirectory, "FOO")) shouldBe None
    }
  }
}
