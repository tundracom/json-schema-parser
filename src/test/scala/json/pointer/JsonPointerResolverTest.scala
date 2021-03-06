package json.pointer

import java.net.URI

import argonaut.Argonaut._
import json.schema.parser.ScalazMatchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scalaz.Success
import scalaz.syntax.std.either._

class JsonPointerResolverTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers with ScalazMatchers {

  val json =
    """
      |{
      | "a": {
      |   "b": 1,
      |   "c": [1,2,3,4]
      | }
      |}
    """.stripMargin.parse.validation.getOrElse(throw new IllegalArgumentException)

  JsonPointerResolver.getClass.toString should " / points to root " in {
    JsonPointerResolver(JsonPointer("").get)(json).validation shouldBe Success(json)
  }

  it should " /<key> points to a node " in {
    JsonPointerResolver(JsonPointer("/a/b").get)(json).validation shouldBe Success(jNumber(1))
  }

  it should " /<key>/<number> points to an array " in {
    JsonPointerResolver(JsonPointer("/a/c/2").get)(json).validation shouldBe Success(jNumber(3))
  }

  it should " fail for /<unknown> " in {
    JsonPointerResolver(JsonPointer("/f").get)(json).validation should containFailure("f not found")
  }

  it should " fail for array index out of bounds /a/c/<unknown> " in {
    JsonPointerResolver(JsonPointer("/a/c/10").get)(json).validation should containFailure("10 not found")
  }

  it should " satisfy example from the JSON-Pointer spec " in {
    val sampleFromSpec =
      """{
      "foo": ["bar", "baz"],
      "": 0,
      "a/b": 1,
      "c%d": 2,
      "e^f": 3,
      "g|h": 4,
      "i\\j": 5,
      "k\"l": 6,
      " ": 7,
      "m~n": 8
    }""".stripMargin.parse.validation.getOrElse(throw new IllegalArgumentException)

    List(
      ("", sampleFromSpec),
      ("/foo", jArrayElements(jString("bar"), jString("baz"))),
      ("/foo/0", jString("bar")),
      ("/", jNumber(0)),
      ("/a~1b", jNumber(1)),
      ("/c%d", jNumber(2)),
      ("/e^f", jNumber(3)),
      ("/g|h", jNumber(4)),
      ("/i\\j", jNumber(5)),
      ("/k\"l", jNumber(6)),
      ("/ ", jNumber(7)),
      ("/m~0n", jNumber(8))
    ) foreach { fe =>
      JsonPointerResolver(JsonPointer(fe._1).get)(sampleFromSpec).validation shouldBe Success(fe._2)
    }

  }

  it should " satisfy URI encoded examples from the JSON-Pointer spec " in {
    val sampleFromSpec =
      """{
      "foo": ["bar", "baz"],
      "": 0,
      "a/b": 1,
      "c%d": 2,
      "e^f": 3,
      "g|h": 4,
      "i\\j": 5,
      "k\"l": 6,
      " ": 7,
      "m~n": 8
    }""".stripMargin.parse.validation.getOrElse(throw new IllegalArgumentException)

    List(
      (new URI("#"), sampleFromSpec),
      (new URI("#/foo"), jArrayElements(jString("bar"), jString("baz"))),
      (new URI("#/foo/0"), jString("bar")),
      (new URI("#/"), jNumber(0)),
      (new URI("#/a~1b"), jNumber(1)),
      (new URI("#/c%25d"), jNumber(2)),
      (new URI("#/e%5Ef"), jNumber(3)),
      (new URI("#/g%7Ch"), jNumber(4)),
      (new URI("#/i%5Cj"), jNumber(5)),
      (new URI("#/k%22l"), jNumber(6)),
      (new URI("#/%20"), jNumber(7)),
      (new URI("#/m~0n"), jNumber(8))
    ) foreach { fe =>
      JsonPointerResolver(fe._1)(sampleFromSpec).validation shouldBe Success(fe._2)
    }

  }

}
