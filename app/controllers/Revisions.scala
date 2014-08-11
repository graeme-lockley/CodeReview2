package controllers

import models.Revision
import play.api.mvc.{Action, Controller}

object Revisions extends Controller {
  def show(revisionID: Long) = Action {
    implicit request =>
      val revision = Revision.find(revisionID).get

      Ok(views.html.revisions.show(revision))
  }
}
