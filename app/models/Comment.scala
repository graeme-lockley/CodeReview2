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
    CommentaryResponse.create(this, comment, author, new java.util.Date())
  }

  def responses(): Traversable[CommentaryResponse] = CommentaryResponse.all(this)
}

object Commentary {
  def create(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Commentary, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    Commentary(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber)
  }

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

object CommentaryResponse {
  def create(commentary: models.Commentary, comment: String, author: models.Author, date: Date): models.CommentaryResponse = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(commentary.id), author.id, commentary.revisionEntry.id, commentary.lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.CommentaryResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    CommentaryResponse(insertDBRevisionEntryFeedback.id, comment, author, date, commentary)
  }

  def all(commentary: Commentary): Traversable[CommentaryResponse] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(commentary.id).map(e => CommentaryResponse(e.id, e.logMessage, Author.get(e.authorID), e.date, commentary))
  }
}

case class Issue(id: IssueID, comment: String, author: Author, date: Date, revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], status: IssueStatus) extends Feedback {
  def addResponse(comment: String, author: Author): IssueResponse = {
    IssueResponse.create(this, comment, author, new java.util.Date())
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

  def responses(): Traversable[IssueResponse] = IssueResponse.all(this)
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

object IssueResponse {
  def create(issue: models.Issue, comment: String, author: models.Author, date: Date): models.IssueResponse = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(issue.id), author.id, issue.revisionEntry.id, issue.lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.IssueResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    IssueResponse(insertDBRevisionEntryFeedback.id, comment, author, date, issue)
  }

  def all(issue: Issue): Traversable[IssueResponse] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(issue.id).map(e => IssueResponse(e.id, e.logMessage, Author.get(e.authorID), e.date, issue))
  }
}
