package controllers

import models.RevisionEntry
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.RevisionEntryDifference

object RevisionEntries extends Controller {
  def diffAgainstPreviousRevision(id: Long) = Action {
    implicit request => {
      val secondRevisionEntry = RevisionEntry.find(id)
      val firstRevisionEntry = if (secondRevisionEntry.isEmpty) None else secondRevisionEntry.get.previousRevisionEntry()

      val differences = RevisionEntryDifference(firstRevisionEntry, secondRevisionEntry)

      Ok(views.html.revisionEntries.diff(firstRevisionEntry, secondRevisionEntry, differences))
    }
  }

  def diffAgainstPreviousRevision(id: Long, lineNumber: Long) = Action {
    implicit request => {
      val secondRevisionEntry = RevisionEntry.find(id)
      val firstRevisionEntry = if (secondRevisionEntry.isEmpty) None else secondRevisionEntry.get.previousRevisionEntry()

      val differences = RevisionEntryDifference(firstRevisionEntry, secondRevisionEntry)

      Ok(views.html.revisionEntries.diff(firstRevisionEntry, secondRevisionEntry, differences))
    }
  }

  def show(id: Long) = Action {
    request =>
      val revisionEntry = RevisionEntry.find(id).get

      Ok(Json.stringify(revisionEntryWriter.write(revisionEntry)))
  }

  def feedback(id: Long) = Action {
    implicit request => {
      val revisionEntry = RevisionEntry.get(id);

      Ok(Json.stringify(feedbackWriter.write(revisionEntry.feedback())))
    }
  }
}
