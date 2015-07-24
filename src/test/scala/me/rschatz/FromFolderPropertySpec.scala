package me.rschatz

import java.io.File
import java.nio.file.Files

import org.scalatest.{Matchers, WordSpec}

class FromFolderPropertySpec extends WordSpec with Matchers with Sandbox {
  "FromFolderProperty" should {
    "figure out the template folder based on default folder property" in {
      val subDir = new File(baseDirectory, "DEFAULT_FOLDER")
      Files.createDirectory(subDir.toPath)

      sys.props += Defaults.templateFolderProperty -> baseDirectory.getPath

      var finder = new FromFolderProperty(Some("DEFAULT_FOLDER"))
      finder.get shouldBe Some(subDir)

      finder = new FromFolderProperty(Some("NON_EXISTING"))
      finder.get shouldBe None

      sys.props -= Defaults.templateFolderProperty
    }
  }

}
