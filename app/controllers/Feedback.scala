package controllers

import models._
import play.api.mvc.Action

object Feedback extends AuthController {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val comment = (jsValue \ "comment").as[String]
      val authorID = (jsValue \ "authorID").as[Long]
      val revisionEntryID = (jsValue \ "revisionEntryID").as[Long]
      val lineNumber = (jsValue \ "lineNumber").as[Long]
      val feedbackStatus = if ((jsValue \ "status").asOpt[String].exists(x => x.equals("open"))) Open() else Closed()

      val revisionEntry = RevisionEntry.find(revisionEntryID).get
      val author = Author.find(authorID).get
      val feedback = revisionEntry.addFeedback(lineNumber, comment, author, feedbackStatus)

      CreateFeedbackEvent(comment, authorID, revisionEntryID, lineNumber, feedbackStatus, feedback.id).publish()

      Ok(feedbackWriter.write(feedback))
  }

  def close(feedbackID: Long, authorID: Long) = Action(parse.json) {
    request =>
      (Author.find(authorID), models.Feedback.find(feedbackID)) match {
        case (Some(author), Some(issue)) =>
          issue.close(author) match {
            case Left(errorMessage) => BadRequest("{\"message\": \"" + errorMessage + "\"}")
            case Right(updatedIssue) =>
              CloseFeedbackEvent(feedbackID, authorID).publish()
              Ok("{}")
          }
        case (Some(_), None) => BadRequest("{\"message\": \"Unknown issue\"}")
        case (None, Some(_)) => BadRequest("{\"message\": \"Unknown author\"}")
        case (None, None) => BadRequest("{\"message\": \"Unknown issue and unknown author\"}")
      }
  }

  def show(feedbackID: Long) = Action {
    request =>
      val feedback = models.Feedback.find(feedbackID).get

      Ok(feedbackWriter.write(feedback))
  }
}
