package me.rschatz

import java.io.File

import org.scalatest.{Matchers, WordSpec}

class FromInputArgsSpec extends WordSpec with Matchers with Sandbox {

  "FromInputArgs" should {
    "figure out the template folder based on input args" in {
      var finder = new FromInputArgs(Some("https://github.com/reikje/npt/archive/master.zip"), tempFolder = baseDirectory)
      finder.get shouldBe Some(new File(baseDirectory, Defaults.downloadDirName))

      finder = new FromInputArgs(Some(baseDirectory.getPath))
      finder.get shouldBe Some(baseDirectory)

      finder = new FromInputArgs(Some(new File(baseDirectory, "NON_EXISTING").getPath))
      finder.get shouldBe None

      finder = new FromInputArgs(Some("https://github.com/reikje/npt/NON-EXISTENT/master.zip"))
      finder.get shouldBe None
    }
  }

}
