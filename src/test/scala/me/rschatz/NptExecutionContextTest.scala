package me.rschatz

import org.scalatest.{FreeSpec, Matchers}
import java.io.File

/**
 * Tests the [[me.rschatz.NptExecutionContext]].
 *
 * @author rschatz
 */
class NptExecutionContextTest extends FreeSpec with Matchers {
  "A NptExecutionContext" - {
    "when created without args " - {
      val context = NptExecutionContext(new File("."))
      "should parse to defaults" in {
        val (orgValue, nameValue, templateValue) = context.inputArgs()
        orgValue should be(Some(me.rschatz.Defaults.organization))
        nameValue should be(Some(me.rschatz.Defaults.name))
        templateValue should be(None)
      }
    }

    "when created with args " - {
      val context = NptExecutionContext(baseDirectory = new File("."), args = Seq("name:foo", "org:bar", "whatever"))
      "should parse properly" in {
        val (_, _, templateValue) = context.inputArgs()
        templateValue should be(Some("whatever"))
      }
    }
  }
}
