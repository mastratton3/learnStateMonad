package com.michaelastratton.learnscalaz

import scalaz._
import Scalaz._

object LearnState {

  case class Record(id: Int, timeSequence: Int, value: Double) { 
      def addGrouping(i: Int): ProcRecord = ProcRecord(id, timeSequence, value, i)
  }
  case class ProcRecord(id: Int, timeSequence: Int, value: Double, grouping: Int)
  case class PrevState(prevSeq: Int, prevGrouping: Int)

  val testList = List(
      Record(1, 1, 1.0),
      Record(2, 1, 1.0),
      Record(3, 2, 1.0),
      Record(4, 2, 2.0)
  )

  def procLines(i: Record): State[PrevState,ProcRecord] = State {
      x =>

      val newGrouping = if (i.timeSequence == x.prevSeq) x.prevGrouping else x.prevGrouping + 1
      (PrevState(i.timeSequence, newGrouping), i.addGrouping(newGrouping))
  }

  val resultInState = testList traverseS(procLines) run (PrevState(0,0))

  // Get the list of processed records out
  val result = resultInState._2

}