package json.schema.scope

import java.net.URI

import argonaut.Argonaut._
import argonaut.{ACursor, HCursor, Json}

import scalaz.{-\/, \/, \/-}

trait ExpandReferences extends JsonTraverser {

  // current scope uri
  type State = URI

  private[scope] def TCheck(tail: TraverseOp): TraverseOp = new TraverseOp {
    override def next(state: State, hc: HCursor): (TraverseState, ACursor) = getId(hc.focus) match {
      case Some(-\/(err)) =>
        ((state, TResult(-\/(err))), hc.acursor)
      case Some(\/-(id)) =>

        val newScope = childScope(state, id)

        // expand id element
        val ac: ACursor = hc.downField("id").withFocus(_ => jString(childScope(state, id).toString)).up

        hc.focus.arrayOrObject(
          ((state, tail), ac),
          array =>
            ((newScope, TArray(array.length - 1, tail)), ac),
          obj =>
            ((newScope, TObject(obj.fieldSet, tail)), ac)
        )

      case None =>

        // expand $ref element
        val ac: ACursor = getRef(hc.focus) match {
          case Some(\/-(id)) => hc.downField("$ref").withFocus(_ => jString(childScope(state, id).toString)).up
          case _ => hc.acursor
        }

        hc.focus.arrayOrObject(
          ((state, tail), ac),
          array =>
            ((state, TArray(array.length - 1, tail)), ac),
          obj =>
            ((state, TObject(obj.fieldSet, tail)), ac)
        )

    }
  }


  private[scope] case class TResult(result: String \/ Json) extends TraverseOp {
    override def next(state: State, hc: HCursor): (TraverseState, ACursor) = ((state, this), hc.failedACursor)
  }

  private[scope] def TReturn: TraverseOp = new TraverseOp {
    override def next(state: State, hc: HCursor): (TraverseState, ACursor) = ((state, TResult(\/-(hc.focus))), hc.acursor)
  }

  def expand(rootScope: URI, hcursor: HCursor): String \/ Json = {
    val init: (State, TraverseOp) = (rootScope, TCheck(TReturn))
    hcursor.traverseUntilDone(init)(treeTraverser) match {
      case (state, TResult(result)) => result
      case _ => -\/("json traversal is incomplete")
    }
  }

}

object ExpandReferences extends ExpandReferences
