package models

import java.sql.Timestamp
import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import ports.DBRevisionEntryFeedbackStatus.DBRevisionEntryFeedbackStatus
import ports.{DBRevisionEntryFeedback, DBRevisionEntryFeedbackStatus, DBRevisionEntryFeedbackType, Library}

case class Feedback(id: FeedbackID, comment: String, author: Author, date: Date, revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], status: FeedbackStatus) {
  def addResponse(comment: String, author: Author): Response = Response.create(this, comment, author, new java.util.Date())

  def responses(): Traversable[Response] = Response.all(this)

  def close(closeAuthor: Author): Either[String, Feedback] = {
    if (status == models.Closed()) Left("This feedback is already closed.")
    else if (author.id != closeAuthor.id) Left("Only the author who opened this feedback may close it.")
    else {
      val updatedIssue = Feedback.update(copy(status = models.Closed()))
      addResponse("Feedback closed", closeAuthor)
      Right(updatedIssue)
    }
  }
}

object Feedback {
  def create(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date, status: FeedbackStatus): Feedback = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Commentary, modelToDB(status))
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)

    Feedback(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber, status)
  }

  def find(feedbackID: FeedbackID): Option[Feedback] = inTransaction {
    Library.revisionEntryComment.lookup(feedbackID) match {
      case Some(comment) => Some(Feedback(feedbackID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber, dbToModel(comment.status)))
      case None => None
    }
  }

  def update(feedback: Feedback): Feedback = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(feedback.id, None, feedback.author.id, feedback.revisionEntry.id, feedback.lineNumber, feedback.comment, new Timestamp(feedback.date.getTime), DBRevisionEntryFeedbackType.Issue, modelToDB(feedback.status))
    Library.revisionEntryComment.update(dbRevisionEntryFeedback)
    feedback
  }

  def all(issue: Feedback): Traversable[Response] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(issue.id).map(e => Response(e.id, e.logMessage, Author.get(e.authorID), e.date, issue))
  }

  def dbToModel(status: DBRevisionEntryFeedbackStatus): FeedbackStatus = status match {
    case DBRevisionEntryFeedbackStatus.Open => models.Open()
    case DBRevisionEntryFeedbackStatus.Closed => models.Closed()
  }
  
  def modelToDB(status: FeedbackStatus): DBRevisionEntryFeedbackStatus = status match {
    case models.Open() => DBRevisionEntryFeedbackStatus.Open 
    case models.Closed() => DBRevisionEntryFeedbackStatus.Closed 
  }
}

case class Response(id: ResponseID, comment: String, author: Author, date: Date, feedback: Feedback)

object Response {
  def create(commentary: models.Feedback, comment: String, author: models.Author, date: Date): models.Response = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(commentary.id), author.id, commentary.revisionEntry.id, commentary.lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.CommentaryResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    Response(insertDBRevisionEntryFeedback.id, comment, author, date, commentary)
  }

  def all(commentary: Feedback): Traversable[Response] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(commentary.id).map(e => Response(e.id, e.logMessage, Author.get(e.authorID), e.date, commentary))
  }
}

trait FeedbackStatus

case class Open() extends FeedbackStatus

case class Closed() extends FeedbackStatus

