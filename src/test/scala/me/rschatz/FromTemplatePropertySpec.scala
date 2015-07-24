package me.rschatz

import java.io.File

import org.scalatest.{Matchers, WordSpec}

class FromTemplatePropertySpec extends WordSpec with Matchers with Sandbox {

  "FromTemplateProperty" should {
    "should figure out the template folder based on default template property" in {
      val finder = new FromTemplateProperty(tempFolder = baseDirectory)
      finder.get shouldBe None

      sys.props.update(Defaults.defaultTemplateProperty, "https://github.com/reikje/npt/archive/master.zip")
      finder.get shouldBe Some(new File(baseDirectory, Defaults.downloadDirName))

      sys.props.update(Defaults.defaultTemplateProperty, baseDirectory.getPath)
      finder.get shouldBe Some(baseDirectory)

      sys.props.update(Defaults.defaultTemplateProperty, "https://github.com/reikje/npt/NON-EXISTENT/master.zip")
      finder.get shouldBe None

      sys.props.update(Defaults.defaultTemplateProperty, new File(baseDirectory, "NON-EXISTING").getPath)
      finder.get shouldBe None
    }
  }
}
