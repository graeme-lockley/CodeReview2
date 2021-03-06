package controllers

import models.RevisionEntry
import play.api.mvc.{Action, Controller}
import services.RevisionEntryDifference

object RevisionEntries extends Controller {
  def diffAgainstPreviousRevision(id: Long) = Action {
    implicit request => {
      val secondRevisionEntry = RevisionEntry.find(id)
      val firstRevisionEntry = if (secondRevisionEntry.isEmpty) None else secondRevisionEntry.get.previousRevisionEntry()

      val differences = RevisionEntryDifference(firstRevisionEntry, secondRevisionEntry)

      Ok(views.html.revisionEntries.diff(firstRevisionEntry, secondRevisionEntry, differences, None))
    }
  }

  def diffAgainstPreviousRevisionOnLine(id: Long, lineNumber: Long) = Action {
    implicit request => {
      val secondRevisionEntry = RevisionEntry.find(id)
      val firstRevisionEntry = if (secondRevisionEntry.isEmpty) None else secondRevisionEntry.get.previousRevisionEntry()

      val differences = RevisionEntryDifference(firstRevisionEntry, secondRevisionEntry)

      Ok(views.html.revisionEntries.diff(firstRevisionEntry, secondRevisionEntry, differences, Some(lineNumber)))
    }
  }

  def show(id: Long) = Action {
    request =>
      val revisionEntry = RevisionEntry.find(id).get

      Ok(revisionEntryWriter.write(revisionEntry))
  }

  def feedback(id: Long) = Action {
    implicit request => {
      val revisionEntry = RevisionEntry.get(id)

      Ok(feedbackWriter.write(revisionEntry.feedback()))
    }
  }
}
