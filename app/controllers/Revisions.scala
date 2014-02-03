package controllers

import play.api.mvc.{Action, Controller}
import models.Repository

object Revisions extends Controller {
    def show(revisionID: Long) = Action {
        implicit request =>
            val revision = Repository.findRevision(revisionID)

            Ok(views.html.revisions.show(revision.get))
    }
}
