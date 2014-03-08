package adaptors

import models._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import scala.collection.mutable
import scala.Some
import java.util.Date
import java.sql.Timestamp
import adaptors.DBRevisionEntryFeedbackStatus.DBRevisionEntryFeedbackStatus


class SQLRepository extends Repository {
    def findAuthor(authorID: AuthorID): Option[Author] = inTransaction {
        DBAuthor.lookup(authorID) match {
            case Some(dbAuthor) => Some(convertDBAuthorToAuthor(dbAuthor))
            case None => None
        }
    }

    def findAuthorOnName(name: String): Option[Author] = inTransaction {
        DBAuthor.lookup(name) match {
            case Some(dbAuthor) => Some(convertDBAuthorToAuthor(dbAuthor))
            case None => None
        }
    }

    def getAuthor(authorID: AuthorID): Author = findAuthor(authorID).get

    private def convertDBAuthorToAuthor(dbAuthor: DBAuthor): Author = Author(dbAuthor.id, dbAuthor.name.getOrElse("(no name)"))

    def findRepoAuthor(repoAuthorID: RepoAuthorID): Option[RepoAuthor] = inTransaction {
        DBRepoAuthor.lookup(repoAuthorID) match {
            case Some(dbRepoAuthor) => Some(convertDBRepoAuthorToRepoAuthor(dbRepoAuthor))
            case None => None
        }
    }

    def getRepoAuthor(repoAuthorID: RepoAuthorID): RepoAuthor = findRepoAuthor(repoAuthorID).get

    private def convertDBRepoAuthorToRepoAuthor(dbRepoAuthor: DBRepoAuthor): RepoAuthor = {
        val author = dbRepoAuthor.authorID match {
            case Some(dbAuthorID) => Some(getAuthor(dbAuthorID))
            case None => None
        }
        RepoAuthor(dbRepoAuthor.id, getRepo(dbRepoAuthor.repoID), author, dbRepoAuthor.repoAuthorName)
    }

    def findAllRepos(): Iterable[Repo] = inTransaction {
        DBRepo.all().map(convertDBRepoToRepo)
    }

    def findRepo(repoID: models.RepoID): Option[Repo] = inTransaction {
        DBRepo.lookup(repoID) match {
            case Some(dbRepo) => Some(convertDBRepoToRepo(dbRepo))
            case None => None
        }
    }

    def getRepo(repoID: models.RepoID): Repo = findRepo(repoID).get

    private def convertDBRepoToRepo(dbRepo: DBRepo): Repo = Repo(dbRepo.id, dbRepo.name, RepoCredentials(dbRepo.svnUser, dbRepo.svnPassword, dbRepo.svnURL))

    def findRevision(revisionID: RevisionID): Option[Revision] = inTransaction {
        val dbRevision = DBRevision.get(revisionID)
        findRepo(dbRevision.repoID) match {
            case Some(repo) => Some(convertDBtoModel(repo, dbRevision, dbRevision.entries()))
            case None => None
        }
    }

    def findRevisionOnRevisionEntryID(revisionEntryID: RevisionEntryID): Option[Revision] = inTransaction {
        DBRevisionEntry.lookup(revisionEntryID) match {
            case Some(dbRevisionEntry) => findRevision(dbRevisionEntry.revisionID)
            case None => None
        }
    }

    def findRevisionEntry(revisionEntryID: models.RevisionEntryID): Option[RevisionEntry] = inTransaction {
        DBRevisionEntry.lookup(revisionEntryID) match {
            case Some(dbRevisionEntry) => Some(convertDBRevisionEntryToModel(findRepo(dbRevisionEntry.repoID).get, dbRevisionEntry))
            case None => None
        }
    }

    def getRevisionEntry(revisionEntryID: models.RevisionEntryID): RevisionEntry = findRevisionEntry(revisionEntryID).get

    def revisionEntryFeedback(revisionEntry: RevisionEntry): Traversable[Feedback] = inTransaction {
        DBRevisionEntryFeedback.directRevisionEntryFeedback(revisionEntry.id).map {
            e =>
                if (e.feedbackType == DBRevisionEntryFeedbackType.Commentary)
                    Commentary(e.id, e.logMessage, getAuthor(e.authorID), e.date, revisionEntry, e.lineNumber)
                else
                    Issue(e.id, e.logMessage, getAuthor(e.authorID), e.date, revisionEntry, e.lineNumber, convertDBRevisionEntryFeedbackType(e.status))
        }
    }


    def findCommentary(commentID: CommentID): Option[Commentary] = inTransaction {
        Library.revisionEntryComment.lookup(commentID) match {
            case Some(comment) =>
                if (comment.feedbackType == DBRevisionEntryFeedbackType.Commentary)
                    Some(Commentary(commentID, comment.logMessage, getAuthor(comment.authorID), comment.date, getRevisionEntry(comment.revisionEntryID), comment.lineNumber))
                else
                    None
            case None => None
        }
    }

    def findIssue(issueID: IssueID): Option[Issue] = inTransaction {
        Library.revisionEntryComment.lookup(issueID) match {
            case Some(comment) =>
                if (comment.feedbackType == DBRevisionEntryFeedbackType.Issue)
                    Some(Issue(issueID, comment.logMessage, getAuthor(comment.authorID), comment.date, getRevisionEntry(comment.revisionEntryID), comment.lineNumber, convertDBRevisionEntryFeedbackType(comment.status)))
                else
                    None
            case None => None
        }
    }

    private def convertDBRevisionEntryFeedbackType(status: DBRevisionEntryFeedbackStatus): IssueStatus = status match {
        case DBRevisionEntryFeedbackStatus.Open => Open()
        case DBRevisionEntryFeedbackStatus.Closed => Closed()
    }

    def getFileRevision(revisionEntryID: RevisionEntryID): String = inTransaction {
        DBRevisionEntryContent.lookup(revisionEntryID) match {
            case Some(dbRevisionEntryContent) => dbRevisionEntryContent.content
            case None =>
                val dbRevisionEntry = DBRevisionEntry.get(revisionEntryID)
                val dbRevision = DBRevision.get(dbRevisionEntry.revisionID)
                val content = SVNRepository.getFileRevision(findRepo(dbRevision.repoID).get, dbRevisionEntry.path, dbRevision.revisionNumber.toInt)

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
		DBRepo.repoAuthors(repo.id).map(ra => convertDBRepoAuthorToRepoAuthor(ra))
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
            if (dbRevision.repoAuthorID.isDefined) Some(getRepoAuthor(dbRevision.repoAuthorID.get)) else None,
            dbRevision.date,
            dbRevision.logMessage,
            dbRevisionEntries.map(dbRevisionEntry => convertDBRevisionEntryToModel(repo, dbRevisionEntry))
        )

    private def convertDBRevisionEntryToModel(repo: Repo, dbRevisionEntry: DBRevisionEntry): RevisionEntry = {
        val entry = dbRevisionEntry.resourceType match {
            case DBResourceType.NoneResource => new NoneEntry(repo, dbRevisionEntry.path)
            case DBResourceType.FileResource => new FileEntry(repo, dbRevisionEntry.path)
            case DBResourceType.DirResource => new DirEntry(repo, dbRevisionEntry.path)
            case DBResourceType.UnknownResource => new UnknownEntry(repo, dbRevisionEntry.path)
        }
        dbRevisionEntry.entryType match {
            case DBEntryType.AddEntry => new AddEntry(dbRevisionEntry.id, entry)
            case DBEntryType.DeleteEntry => new DeleteEntry(dbRevisionEntry.id, entry)
            case DBEntryType.ModifyEntry => new ModifiedEntry(dbRevisionEntry.id, entry)
            case DBEntryType.ReplaceEntry => new ReplacedEntry(dbRevisionEntry.id, entry, dbRevisionEntry.copyPath.get, dbRevisionEntry.copyRevision.get)
        }
    }

    def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary = inTransaction {
        val dbRevisionEntryFeedback = new DBRevisionEntryFeedback(UNKNOWN_REVISION_ENTRY_FEEDBACK_ID, None, author.id, revisionEntry.id, lineNumber, comment, new Timestamp(date.getTime), DBRevisionEntryFeedbackType.Commentary, DBRevisionEntryFeedbackStatus.Closed)
        val insertDBRevisionEntryFeedback = Library.revisionEntryComment.insert(dbRevisionEntryFeedback)
        Commentary(insertDBRevisionEntryFeedback.id, comment, author, date, revisionEntry, lineNumber)
    }

    def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse] = inTransaction {
        DBRevisionEntryFeedback.childrenByDate(commentary.id).map(e => CommentaryResponse(e.id, e.logMessage, getAuthor(e.authorID), e.date, commentary))
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
        DBRevisionEntryFeedback.childrenByDate(issue.id).map(e => IssueResponse(e.id, e.logMessage, getAuthor(e.authorID), e.date, issue))
    }
}
