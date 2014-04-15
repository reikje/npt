package me.rschatz

import org.scalatest.{FreeSpec, Matchers}
import org.apache.commons.io.FileUtils
/**
 * Tests the [[me.rschatz.NptExecutionContext]].
 *
 * @author rschatz
 */
class NptExecutionContextTest extends FreeSpec with Matchers {
    "A NptExecutionContext" - {
        "when created without args " - {
            val context = new NptExecutionContext(null, Seq(), null)
            "should parse to defaults" in {
                val (orgValue, nameValue, templateValue) = context.inputArgs()
                orgValue should be (Some(me.rschatz.Defaults.organization))
                nameValue should be (Some(me.rschatz.Defaults.name))
                templateValue should be (None)
            }
        }

        "when created with args " - {
            val context = new NptExecutionContext(null, Seq("name:foo", "org:bar", "whatever"), null)
            "should parse properly" in {
                val (orgValue, nameValue, templateValue) = context.inputArgs()
                templateValue should be (Some("whatever"))
            }
        }
    }
}
