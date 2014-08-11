package models

import java.sql.Timestamp
import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import ports.DBRevisionEntryFeedbackStatus._
import ports.{DBRevisionEntryFeedback, DBRevisionEntryFeedbackStatus, DBRevisionEntryFeedbackType, Library}

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

object Commentary {
  def find(commentID: CommentID): Option[Commentary] = inTransaction {
    Library.revisionEntryComment.lookup(commentID) match {
      case Some(comment) =>
        if (comment.feedbackType == DBRevisionEntryFeedbackType.Commentary)
          Some(Commentary(commentID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber))
        else
          None
      case None => None
    }
  }
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
      val updatedIssue = Issue.update(copy(status = models.Closed()))
      addResponse("Issue closed", closeAuthor)
      Right(updatedIssue)
    }
  }

  def responses(): Traversable[IssueResponse] = Repository.issueResponses(this)
}

object Issue {
  def find(issueID: IssueID): Option[Issue] = inTransaction {
    Library.revisionEntryComment.lookup(issueID) match {
      case Some(comment) =>
        if (comment.feedbackType == DBRevisionEntryFeedbackType.Issue)
          Some(Issue(issueID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber, Feedback.dbToModel(comment.status)))
        else
          None
      case None => None
    }
  }

  def create(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Issue = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Issue, DBRevisionEntryFeedbackStatus.Open)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    Issue(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber, models.Closed())
  }

  def update(issue: Issue): Issue = inTransaction {
    val dbRevisionEntryFeedbackStatus = issue.status match {
      case Open() => DBRevisionEntryFeedbackStatus.Open
      case Closed() => DBRevisionEntryFeedbackStatus.Closed
    }
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(issue.id, None, issue.author.id, issue.revisionEntry.id, issue.lineNumber, issue.comment, new Timestamp(issue.date.getTime), DBRevisionEntryFeedbackType.Issue, dbRevisionEntryFeedbackStatus)
    Library.revisionEntryComment.update(dbRevisionEntryFeedback)
    issue
  }
}

case class IssueResponse(id: IssueResponseID, comment: String, author: Author, date: Date, issue: Issue) extends Feedback
