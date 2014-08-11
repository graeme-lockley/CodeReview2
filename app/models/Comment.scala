package models

import java.util.Date

import ports.DBRevisionEntryFeedbackStatus
import ports.DBRevisionEntryFeedbackStatus._

trait Feedback {
  val id: CommentID
  val comment: String
  val author: Author
  val date: Date
}

object Feedback {
  def dbToModel(status: DBRevisionEntryFeedbackStatus): IssueStatus = status match {
    case DBRevisionEntryFeedbackStatus.Open => models.Open()
    case DBRevisionEntryFeedbackStatus.Closed => models.Closed()
  }
}

case class Commentary(id: CommentID, comment: String, author: Author, date: Date, revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType]) extends Feedback {
  def addResponse(comment: String, author: Author): CommentaryResponse = {
    Repository.createCommentaryResponse(this, comment, author, new java.util.Date())
  }

  def responses(): Traversable[CommentaryResponse] = Repository.commentaryResponses(this)
}

trait IssueStatus

case class Open() extends IssueStatus

case class Closed() extends IssueStatus

case class CommentaryResponse(id: CommentResponseID, comment: String, author: Author, date: Date, commentary: Commentary) extends Feedback

case class Issue(id: IssueID, comment: String, author: Author, date: Date, revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], status: IssueStatus) extends Feedback {
  def addResponse(comment: String, author: Author): IssueResponse = {
    Repository.createIssueResponse(this, comment, author, new java.util.Date())
  }

  def close(closeAuthor: Author): Either[String, Issue] = {
    if (status == models.Closed()) Left("This issue has already been closed.")
    else if (author.id != closeAuthor.id) Left("Only the author who opened the issue may close the issue.")
    else {
      val updatedIssue = Repository.updateIssue(copy(status = models.Closed()))
      addResponse("Issue closed", closeAuthor)
      Right(updatedIssue)
    }
  }

  def responses(): Traversable[IssueResponse] = Repository.issueResponses(this)
}

case class IssueResponse(id: IssueResponseID, comment: String, author: Author, date: Date, issue: Issue) extends Feedback
