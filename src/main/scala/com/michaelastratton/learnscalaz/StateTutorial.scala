package com.michaelastratton.learnscalaz

import scalaz._
import Scalaz._

object StateTutorial {

  /** Sample data for quick testing */
  val testValues = List(
  Some(1.0),
  None,
  Some(2.0),
  None,
  None,
  None,
  None,
  None,
  None,
  None,
  None,
  None,
  None,
  Some(15.0)
  )
  val testList = (1 to 14).toList.zip(testValues).map(x => RawRow(x._1,x._2))

  case class Row(time: Int, value: Option[Double]) 
  case class RawRow(time: Int, value: Option[Double]) {
    def toRow: Row = Row(time, value)
  }

  /** Old way of maintaing state in a way that is still pure for the caller */
  def processRawRow(ls: List[RawRow], threshold: Int = 10): List[Row] = {
    var secondsBetween = 0
    var priorMeasurement: Option[Double] = None // Explicit typing required since Scala won't be able to infer this 
    ls.map{
      elem => 
      elem match {
        case RawRow(_, Some(x)) => {
          secondsBetween = 0
          priorMeasurement = elem.value
          elem.toRow
        }
        case RawRow(t, None) if (secondsBetween + 1 < threshold) => {
          secondsBetween += 1
          elem.copy(value = priorMeasurement).toRow
        }
        case _ => {
          secondsBetween += 1
          elem.toRow
        }
      }
    }
  }

  /* Monadic way to maintain state while traversing */
  case class PrevState(timeBetween: Int, prevValue: Option[Double])

  def procLines(i: RawRow, timeLimit: Int): State[PrevState,Row] = State {
    x: PrevState =>
    val newTime = x.timeBetween + 1

    i match {
      case RawRow(_, Some(y)) => (PrevState(0, Some(y)), i.toRow)
      case RawRow(_, None) if newTime < timeLimit => (PrevState(newTime,x.prevValue),i.copy(value = x.prevValue).toRow)
      case _ => (PrevState(newTime, x.prevValue), i.toRow)
    }
  } 

  val result = testList traverseS(procLines(_, 10)) run (PrevState(0,None: Option[Double]))

}