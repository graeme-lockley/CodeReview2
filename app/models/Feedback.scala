package models

import java.sql.Timestamp
import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import ports.DBRevisionEntryFeedbackStatus.DBRevisionEntryFeedbackStatus
import ports.{DBRevisionEntryFeedback, DBRevisionEntryFeedbackStatus, DBRevisionEntryFeedbackType, Library}

case class Feedback(id: FeedbackID, comment: String, author: Author, date: Date, revisionEntry: RevisionEntry, lineNumber: LineNumberType, status: FeedbackStatus) {
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
  def create(revisionEntry: RevisionEntry, lineNumber: LineNumberType, comment: String, author: Author, date: Date, status: FeedbackStatus): Feedback = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, Some(lineNumber), comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Commentary, modelToDB(status))
    val insertDBRevisionEntryFeedback = Library.revisionEntryFeedback.insert(dbRevisionEntryFeedback)

    Feedback(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber, status)
  }

  def find(feedbackID: FeedbackID): Option[Feedback] = inTransaction {
    Library.revisionEntryFeedback.lookup(feedbackID) match {
      case Some(comment) => Some(Feedback(feedbackID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber.get, dbToModel(comment.status)))
      case None => None
    }
  }

  def update(feedback: Feedback): Feedback = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(feedback.id, None, feedback.author.id, feedback.revisionEntry.id, Some(feedback.lineNumber), feedback.comment, new Timestamp(feedback.date.getTime), DBRevisionEntryFeedbackType.Issue, modelToDB(feedback.status))
    Library.revisionEntryFeedback.update(dbRevisionEntryFeedback)
    feedback
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
  def create(feedback: models.Feedback, comment: String, author: models.Author, date: Date): models.Response = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(feedback.id), author.id, feedback.revisionEntry.id, Some(feedback.lineNumber), comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.CommentaryResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryFeedback.insert(dbRevisionEntryFeedback)
    Response(insertDBRevisionEntryFeedback.id, comment, author, date, feedback)
  }

  def all(feedback: Feedback): Traversable[Response] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(feedback.id).map(e => Response(e.id, e.logMessage, Author.get(e.authorID), e.date, feedback))
  }
}

trait FeedbackStatus

case class Open() extends FeedbackStatus

case class Closed() extends FeedbackStatus

