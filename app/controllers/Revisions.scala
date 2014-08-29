package controllers

import models.Revision
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean
import play.api.libs.json.Json
import play.api.mvc.{Action, RequestHeader, SimpleResult}
import ports.{DBReviewStatus, DBRevision}

object Revisions extends AuthController {
  var queries: Map[String, DBRevision => BinaryOperatorNodeLogicalBoolean] = Map()

  queries = queries
    .+(("inProgress", (r: DBRevision) => r.reviewStatus in Set(DBReviewStatus.InProgress)))
    .+(("outstanding", (r: DBRevision) => r.reviewStatus in Set(DBReviewStatus.Outstanding)))
    .+(("complete", (r: DBRevision) => r.reviewStatus in Set(DBReviewStatus.Complete)))

  def list = Action {
    implicit request =>
      val query = request.getQueryString("query")
      val all =
        if (query.getOrElse("").equals("inProgressMine"))
          Revision.all((r: DBRevision) => r.reviewStatus in Set(DBReviewStatus.InProgress) and r.reviewAuthorID === loggedOnUser.map(x => x.id))
        else if (query.isEmpty)
          Revision.all()
        else
          Revision.all(queries.get(query.get).get)

      Ok(Json.stringify(revisionWriter.write(all)))
  }

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
    implicit request => applyReviewAction(revisionID, x => x.startReview(loggedOnUser))
  }

  def cancelReview(revisionID: Long) = Action {
    implicit request => applyReviewAction(revisionID, x => x.cancelReview(loggedOnUser))
  }

  def completeReview(revisionID: Long) = Action {
    implicit request => applyReviewAction(revisionID, x => x.completeReview(loggedOnUser))
  }

  private def applyReviewAction(revisionID: Long, action: Revision => Either[String, Revision])(implicit request: RequestHeader): SimpleResult =
    action(Revision.find(revisionID).get) match {
      case Left(l) => PreconditionFailed(l)
      case Right(r) =>
        Revision.save(r)
        showRevision(r)
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
