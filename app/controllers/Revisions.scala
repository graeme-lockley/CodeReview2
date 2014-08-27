package controllers

import models.Revision
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

object Revisions extends Controller {
  def showAsHTML(revisionID: Long) = Action {
    implicit request =>
      val revision = Revision.find(revisionID).get

      Ok(views.html.revisions.show(revision))
  }

  def show(revisionID: Long) = Action {
    request =>
      val revision = Revision.find(revisionID).get

      Ok(Json.stringify(revisionWriter.write(revision)))
  }
}
