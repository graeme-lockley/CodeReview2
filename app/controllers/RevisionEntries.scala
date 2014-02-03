package controllers

import play.api.mvc.{Action, Controller}
import models.{NullRevisionEntry, Repository}
import services.RevisionEntryDifference
import play.api.mvc.{Action, Controller}
import models.Repository
import play.api.libs.json.Json

object RevisionEntries extends Controller {
    def diffAgainstPreviousRevision(id: Long) = Action {
        implicit request => {
            val secondRevisionEntry = Repository.findRevisionEntry(id)
            val firstRevisionEntry = secondRevisionEntry.getOrElse(NullRevisionEntry).previousRevisionEntry()

            val differences = RevisionEntryDifference(firstRevisionEntry.getOrElse(NullRevisionEntry), secondRevisionEntry.getOrElse(NullRevisionEntry))

            Ok(views.html.revisionEntries.diff(firstRevisionEntry.getOrElse(NullRevisionEntry), secondRevisionEntry.getOrElse(NullRevisionEntry), differences))
        }
    }

    def show(id: Long) = Action {
      request =>
            val revisionEntry = Repository.findRevisionEntry(id).get

            Ok(Json.stringify(revisionEntryWriter.write(revisionEntry)))
    }
}
