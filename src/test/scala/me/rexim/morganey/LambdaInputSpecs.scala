package me.rexim.morganey

import me.rexim.morganey.ast._
import me.rexim.morganey.church.ChurchPairConverter._
import me.rexim.morganey.church.ChurchNumberConverter._
import me.rexim.morganey.helpers.TestTerms
import org.scalatest._

class LambdaInputSpecs extends FlatSpec with Matchers with TestTerms {
  val input = LambdaInput("khooy".toStream)

  "Lambda input" should "has zero free vars by definition" in {
    input.freeVars.isEmpty should be (true)
  }

  "Lambda input" should "lazily evaluated to a pair when substituted" in {
    val forcedInput = decodePair(input.substitute(LambdaVar("x") -> LambdaVar("x")))
    forcedInput.isDefined should be (true)

    val firstChar = decodeChar(forcedInput.get._1)
    firstChar should be (Some('k'))
  }
}
