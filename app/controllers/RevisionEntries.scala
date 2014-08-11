package controllers

import models.{NullRevisionEntry, RevisionEntry}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.RevisionEntryDifference

object RevisionEntries extends Controller {
  def diffAgainstPreviousRevision(id: Long) = Action {
    implicit request => {
      val secondRevisionEntry = RevisionEntry.find(id)
      val firstRevisionEntry = secondRevisionEntry.getOrElse(NullRevisionEntry).previousRevisionEntry()

      val differences = RevisionEntryDifference(firstRevisionEntry.getOrElse(NullRevisionEntry), secondRevisionEntry.getOrElse(NullRevisionEntry))

      Ok(views.html.revisionEntries.diff(firstRevisionEntry.getOrElse(NullRevisionEntry), secondRevisionEntry.getOrElse(NullRevisionEntry), differences))
    }
  }

  def show(id: Long) = Action {
    request =>
      val revisionEntry = RevisionEntry.find(id).get

      Ok(Json.stringify(revisionEntryWriter.write(revisionEntry)))
  }
}
