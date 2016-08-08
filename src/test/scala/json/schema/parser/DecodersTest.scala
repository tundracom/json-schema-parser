package json.schema.parser

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scalaz.{Failure, Success}

class DecodersTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers with Decoders {

  import argonaut.Argonaut._

  implicit val simpleTypes: Gen[SimpleType.Value] = Gen.oneOf(SimpleType.values.toList)

  val oneOrListOfStrings = oneOrNonEmptyList[String]

  SimpleType.getClass.toString should "encode and decode" in {
    forAll(simpleTypes) { (c: SimpleType.SimpleType) =>
      c.asJson.nospaces.decodeValidation[SimpleType.SimpleType] shouldBe Success(c)
    }
  }

  "OneOrMoreStrings" should "decode a list into a list" in {
    """
      |["string1", "string2"]
    """.stripMargin.decodeValidation[List[String]](oneOrListOfStrings) shouldBe Success(List("string1", "string2"))
  }

  it should "decode a single item into a list" in {
    """
      |"string1"
    """.stripMargin.decodeValidation[List[String]](oneOrListOfStrings) shouldBe Success(List("string1"))
  }

  "SetDecodeJsonStrict" should "decode a valid set into a set" in {
    """
      |["string1", "string2"]
    """.stripMargin.decodeValidation[Set[String]](setDecodeJsonStrict) shouldBe Success(Set("string1", "string2"))
  }

  it should "fail decode a invalid set" in {
    """
      |["string1", "string2", "string1"]
    """.stripMargin.decodeValidation[Set[String]](setDecodeJsonStrict) shouldBe Failure("[A]Set[A]: []")
  }

  "NonEmptySetDecodeJsonStrict" should "decode a valid set into a set" in {
    """
      |["string1", "string2"]
    """.stripMargin.decodeValidation[Set[String]](nonEmptySetDecodeJsonStrict) shouldBe Success(Set("string1", "string2"))
  }
  it should "fail decode an empty list" in {
    """
      []
    """.stripMargin.decodeValidation[Set[String]](nonEmptySetDecodeJsonStrict) shouldBe Failure("[A]Set[A]: []")
  }

}