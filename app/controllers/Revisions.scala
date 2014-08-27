package controllers

import models.Revision
import play.api.libs.json.Json
import play.api.mvc.{Action, RequestHeader}

object Revisions extends AuthController {
  def showAsHTML(revisionID: Long) = Action {
    implicit request =>
      val revision = Revision.find(revisionID).get

      Ok(views.html.revisions.show(revision))
  }

  def show(revisionID: Long) = Action {
    implicit request =>
      val revision = Revision.find(revisionID).get

      showRevision(revision)
  }

  def startReview(revisionID: Long) = Action {
    implicit request =>
      Revision.find(revisionID).get.startReview(loggedOnUser) match {
        case Left(l) => PreconditionFailed(l)
        case Right(r) =>
          Revision.save(r)
          showRevision(r)
      }
  }

  def cancelReview(revisionID: Long) = Action {
    implicit request =>
      Revision.find(revisionID).get.cancelReview(loggedOnUser) match {
        case Left(l) => PreconditionFailed(l)
        case Right(r) =>
          Revision.save(r)
          showRevision(r)
      }
  }

  def completeReview(revisionID: Long) = Action {
    implicit request =>
      Revision.find(revisionID).get.startReview(loggedOnUser) match {
        case Left(l) => PreconditionFailed(l)
        case Right(r) =>
          Revision.save(r)
          showRevision(r)
      }
  }

  private def showRevision(revision: Revision)(implicit request: RequestHeader) = {
    val verbs = Verbs.builder()
      .addIf(revision.canReview(loggedOnUser), "Review", routes.Revisions.startReview(revision.id))
      .addIf(revision.canComplete(loggedOnUser), "Cancel", routes.Revisions.cancelReview(revision.id))
      .addIf(revision.canComplete(loggedOnUser), "Complete", routes.Revisions.completeReview(revision.id))
      .create
    Ok(Json.stringify(revisionWriter.write(revision, verbs)))
  }
}
