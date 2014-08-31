package services

import java.io.{BufferedReader, StringReader}
import java.util

import difflib.Delta.TYPE
import difflib.{Delta, DiffUtils}
import models.RevisionEntry

import scala.collection.JavaConverters._

class RevisionEntryDifference(val fromRevisionEntry: Option[RevisionEntry], val toRevisionEntry: Option[RevisionEntry]) {
  val fromRevisionContent = stringToLines(fromRevisionEntry.map(_.content()).getOrElse(""))
  val toRevisionContent = stringToLines(toRevisionEntry.map(_.content()).getOrElse(""))

  def difference(): util.List[Delta] = {
    val patch = DiffUtils.diff(fromRevisionContent, toRevisionContent)
    patch.getDeltas
  }

  def entryDifferences(): util.ArrayList[EntryLine] =
    new EntryDifferences(fromRevisionContent, toRevisionContent).entryLines()

  private def stringToLines(source: String): util.List[String] = {
    val lines = new util.LinkedList[String]()

    val in = new BufferedReader(new StringReader(source))
    var line = in.readLine
    while (line != null) {
      lines.add(line)
      line = in.readLine
    }
    lines
  }

  override def toString: String = (fromRevisionEntry, toRevisionEntry).toString()
}

class EntryDifferences(val fromList: util.List[String], val toList: util.List[String]) {
  var fromListLineNumber: Int = 0
  val fromListLength: Int = fromList.size()
  var toListLineNumber: Int = 0
  val toListLength: Int = toList.size()

  val patch = DiffUtils.diff(fromList, toList)
  val differences: util.List[Delta] = patch.getDeltas

  var result: java.util.ArrayList[EntryLine] = new java.util.ArrayList[EntryLine]()

  private def currentFromLine: String =
    if (fromListLineNumber < fromListLength) fromList.get(fromListLineNumber) else ""

  private def currentToLine: String =
    if (toListLineNumber < toListLength) toList.get(toListLineNumber) else ""

  private def addEntryLine(entryLine: EntryLine, fromLineNumberIncrement: Int, toLineNumberIncrement: Int): Unit = {
    result.add(entryLine)
    fromListLineNumber += fromLineNumberIncrement
    toListLineNumber += toLineNumberIncrement
  }

  private def dumpLines(): Unit =
    while (fromListLineNumber < fromListLength || toListLineNumber < toListLength) {
      addEntryLine(new EntryLine(currentFromLine, fromListLineNumber, 0, currentToLine, toListLineNumber, 0), 1, 1)
    }

  private def dumpToLines(toLineNumber: Int): Unit =
    while (fromListLineNumber < toLineNumber) {
      addEntryLine(new EntryLine(currentFromLine, fromListLineNumber, 0, currentToLine, toListLineNumber, 0), 1, 1)
    }

  def entryLines(): util.ArrayList[EntryLine] = {
    for (difference: Delta <- differences.asScala) {
      difference.getType match {
        case TYPE.CHANGE =>
          dumpToLines(difference.getOriginal.getPosition)

          for (lp <- 1 to Math.min(difference.getOriginal.getLines.size(), difference.getRevised.getLines.size())) {
            addEntryLine(new EntryLine(currentFromLine, fromListLineNumber, 3, currentToLine, toListLineNumber, 3), 1, 1)
          }
          if (difference.getOriginal.getLines.size() > difference.getRevised.getLines.size()) {
            for (lp <- Math.min(difference.getOriginal.getLines.size(), difference.getRevised.getLines.size()) + 1 to difference.getOriginal.getLines.size()) {
              addEntryLine(new EntryLine(currentFromLine, fromListLineNumber, 3, "", -1, -1), 1, 0)
            }
          } else {
            for (lp <- Math.min(difference.getOriginal.getLines.size(), difference.getRevised.getLines.size()) + 1 to difference.getRevised.getLines.size()) {
              addEntryLine(new EntryLine("", -1, -1, currentToLine, toListLineNumber, 3), 0, 1)
            }
          }
        case TYPE.DELETE =>
          dumpToLines(difference.getOriginal.getPosition)

          for (deletedLine <- difference.getOriginal.getLines.asScala) {
            addEntryLine(new EntryLine(currentFromLine, fromListLineNumber, 1, "", -1, -1), 1, 0)
          }
        case TYPE.INSERT =>
          dumpToLines(difference.getOriginal.getPosition)

          for (insertedLine <- difference.getRevised.getLines.asScala) {
            addEntryLine(new EntryLine("", -1, -1, currentToLine, toListLineNumber, 2), 0, 1)
          }
      }
    }

    dumpLines()

    result
  }
}

class EntryLine(val fromLine: String, val fromLineNumber: Int, val fromLineAnnotation: Int, val toLine: String, val toLineNumber: Int, val toLineAnnotation: Int) {
  def fromLineNumberAsString: String = if (fromLineNumber < 0) "" else (fromLineNumber + 1).toString

  def toLineNumberAsString: String = if (toLineNumber < 0) "" else (toLineNumber + 1).toString
}

object RevisionEntryDifference {
  def apply(fromRevisionEntry: Option[RevisionEntry], toRevisionEntry: Option[RevisionEntry]): RevisionEntryDifference = new RevisionEntryDifference(fromRevisionEntry, toRevisionEntry)
}