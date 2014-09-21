package controllers

import models.{Author, Closed, Open, RevisionEntry}
import play.api.mvc.Action

object Feedback extends AuthController {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val comment = (jsValue \ "comment").as[String]
      val authorID = (jsValue \ "authorID").as[Long]
      val revisionEntryID = (jsValue \ "revisionEntryID").as[Long]
      val lineNumber = (jsValue \ "lineNumber").asOpt[Long]
      val feedbackStatus = if ((jsValue \ "status").asOpt[String].exists(x => x.equals("open"))) Open() else Closed()

      val revisionEntry = RevisionEntry.find(revisionEntryID).get
      val author = Author.find(authorID).get
      val feedback = revisionEntry.addFeedback(lineNumber, comment, author, feedbackStatus)

      Ok(feedbackWriter.write(feedback))
  }

  def close(issueID: Long, authorID: Long) = Action(parse.json) {
    request =>
      (Author.find(authorID), models.Feedback.find(issueID)) match {
        case (Some(author), Some(issue)) =>
          issue.close(author) match {
            case Left(errorMessage) => BadRequest("{\"message\": \"" + errorMessage + "\"}")
            case Right(updatedIssue) => Ok("{}")
          }
        case (Some(_), None) => BadRequest("{\"message\": \"Unknown issue\"}")
        case (None, Some(_)) => BadRequest("{\"message\": \"Unknown author\"}")
        case (None, None) => BadRequest("{\"message\": \"Unknown issue and unknown author\"}")
      }
  }

  def show(issueID: Long) = Action {
    request =>
      val feedback = models.Feedback.find(issueID).get

      Ok(feedbackWriter.write(feedback))
  }
}
