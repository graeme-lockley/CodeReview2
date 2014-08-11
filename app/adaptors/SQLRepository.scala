package adaptors

import java.sql.Timestamp
import java.util.Date

import models._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import ports._

import scala.collection.mutable

class SQLRepository extends Repository {
  def findCommentary(commentID: CommentID): Option[Commentary] = inTransaction {
    Library.revisionEntryComment.lookup(commentID) match {
      case Some(comment) =>
        if (comment.feedbackType == DBRevisionEntryFeedbackType.Commentary)
          Some(Commentary(commentID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber))
        else
          None
      case None => None
    }
  }

  def findIssue(issueID: IssueID): Option[Issue] = inTransaction {
    Library.revisionEntryComment.lookup(issueID) match {
      case Some(comment) =>
        if (comment.feedbackType == DBRevisionEntryFeedbackType.Issue)
          Some(Issue(issueID, comment.logMessage, Author.get(comment.authorID), comment.date, RevisionEntry.get(comment.revisionEntryID), comment.lineNumber, Feedback.dbToModel(comment.status)))
        else
          None
      case None => None
    }
  }

  def getFileRevision(revisionEntryID: RevisionEntryID): String = inTransaction {
    DBRevisionEntryContent.lookup(revisionEntryID) match {
      case Some(dbRevisionEntryContent) => dbRevisionEntryContent.content
      case None =>
        val dbRevisionEntry = DBRevisionEntry.get(revisionEntryID)
        val dbRevision = DBRevision.get(dbRevisionEntry.revisionID)
        val content = SVNRepository.getFileRevision(Repo.find(dbRevision.repoID).get, dbRevisionEntry.path, dbRevision.revisionNumber.toInt)

        try {
          Library.revisionEntriesContent.insert(new DBRevisionEntryContent(dbRevisionEntry.id, content))
        } catch {
          case _: Exception => ()
        }

        content
    }
  }

  def refreshVCS(repo: Repo): Unit = SVNRepository.refresh(repo)

  def repoRevisions(repo: Repo): Traversable[Revision] = inTransaction {
    val dbRepo = DBRepo.get(repo.id)

    convertToRevisions(repo, dbRepo.revisions())
  }

  def repoAuthors(repo: Repo): Traversable[RepoAuthor] = inTransaction {
    DBRepo.repoAuthors(repo.id).map(ra => RepoAuthor.dbToModel(ra))
  }

  def entryRevisions(repo: Repo, path: String): Traversable[Revision] = {
    inTransaction {
      val dbRepo = DBRepo.get(repo.id)
      convertToRevisions(repo, dbRepo.entryRevisions(path))
    }
  }

  private def convertToRevisions(repo: Repo, dbQueryResult: Query[(DBRevision, DBRevisionEntry)]): Traversable[Revision] = {
    val result = new mutable.HashMap[DBRevision, mutable.ListBuffer[DBRevisionEntry]]()
    for ((x, y) <- dbQueryResult) {
      val key = result.get(x)
      if (key.isDefined) {
        key.get += y
      } else {
        val value = new mutable.ListBuffer[DBRevisionEntry]()
        value += y
        result.put(x, value)
      }
    }

    result.map(x => {
      val dbRevision = x._1
      val dbRevisionEntries = x._2
      convertDBtoModel(repo, dbRevision, dbRevisionEntries)
    })
  }

  private def convertDBtoModel(repo: Repo, dbRevision: DBRevision, dbRevisionEntries: Iterable[DBRevisionEntry]): Revision =
    new Revision(
      dbRevision.id,
      repo,
      dbRevision.revisionNumber,
      if (dbRevision.repoAuthorID.isDefined) Some(RepoAuthor.get(dbRevision.repoAuthorID.get)) else None,
      dbRevision.date,
      dbRevision.logMessage,
      dbRevisionEntries.map(dbRevisionEntry => RevisionEntry.dbToModel(repo, dbRevisionEntry))
    )

  def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Commentary, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    Commentary(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber)
  }

  def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(commentary.id).map(e => CommentaryResponse(e.id, e.logMessage, Author.get(e.authorID), e.date, commentary))
  }

  def createCommentaryResponse(commentary: models.Commentary, comment: String, author: models.Author, date: Date): models.CommentaryResponse = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(commentary.id), author.id, commentary.revisionEntry.id, commentary.lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.CommentaryResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    CommentaryResponse(insertDBRevisionEntryFeedback.id, comment, author, date, commentary)
  }

  def createIssue(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Issue = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Issue, DBRevisionEntryFeedbackStatus.Open)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    Issue(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber, Closed())
  }

  def updateIssue(issue: Issue): Issue = inTransaction {
    val dbRevisionEntryFeedbackStatus = issue.status match {
      case Open() => DBRevisionEntryFeedbackStatus.Open
      case Closed() => DBRevisionEntryFeedbackStatus.Closed
    }
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(issue.id, None, issue.author.id, issue.revisionEntry.id, issue.lineNumber, issue.comment, new Timestamp(issue.date.getTime), DBRevisionEntryFeedbackType.Issue, dbRevisionEntryFeedbackStatus)
    Library.revisionEntryComment.update(dbRevisionEntryFeedback)
    issue
  }

  def createIssueResponse(issue: models.Issue, comment: String, author: models.Author, date: Date): models.IssueResponse = inTransaction {
    val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, Some(issue.id), author.id, issue.revisionEntry.id, issue.lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.IssueResponse, DBRevisionEntryFeedbackStatus.Closed)
    val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
    IssueResponse(insertDBRevisionEntryFeedback.id, comment, author, date, issue)
  }

  def issueResponses(issue: Issue): Traversable[IssueResponse] = inTransaction {
    DBRevisionEntryFeedback.childrenByDate(issue.id).map(e => IssueResponse(e.id, e.logMessage, Author.get(e.authorID), e.date, issue))
  }
}
