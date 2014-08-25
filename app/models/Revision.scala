package models

import java.util.Date

import adaptors.SVNRepository
import org.squeryl.PrimitiveTypeMode._
import ports._

sealed trait Entry {
  val repo: Repo
  val path: String

  def revisions(): Traversable[Revision] = Entry.entryRevisions(repo, path)
}

object NullEntry extends Entry {
  val repo: Repo = NullRepo
  val path: String = "/"
}

case class NoneEntry(repo: Repo, path: String) extends Entry {
  override def toString: String = "None: " + path
}

case class FileEntry(repo: Repo, path: String) extends Entry {
  override def toString: String = "File: " + path
}

case class DirEntry(repo: Repo, path: String) extends Entry {
  override def toString: String = "Dir: " + path
}

case class UnknownEntry(repo: Repo, path: String) extends Entry {
  override def toString: String = "Unknown: " + path
}

object Entry {
  def apply(repo: Repo, path: String): Entry = {
    new UnknownEntry(repo, path)
  }

  def entryRevisions(repo: Repo, path: String): Traversable[Revision] = {
    inTransaction {
      val dbRepo = DBRepo.get(repo.id)
      Repo.convertToRevisions(repo, dbRepo.entryRevisions(path))
    }
  }
}

sealed trait RevisionEntry {
  val id: RevisionEntryID
  val entry: Entry

  def operation: String

  override def toString: String = operation + ": (" + entry + ")"

  def revisionNumber: RevisionNumber = {
    Revision.findRevisionOnRevisionEntryID(id) match {
      case Some(revision) => revision.revisionNumber
      case None => UNKNOWN_REVISION_NUMBER
    }
  }

  def revision: Revision = Revision.findRevisionOnRevisionEntryID(id).get

  def previousRevisionEntry(): Option[RevisionEntry] = {
    def calcFromRevision(): Revision = {
      def _fromRevision(currentFromRevision: Revision, revisions: Traversable[Revision]): Revision = {
        if (revisions.isEmpty)
          currentFromRevision
        else {
          val r = revisions.head
          val rs = revisions.tail

          if (r.revisionNumber > currentFromRevision.revisionNumber && r.revisionNumber < revisionNumber)
            _fromRevision(r, rs)
          else
            _fromRevision(currentFromRevision, rs)
        }
      }
      _fromRevision(Revision(), entry.revisions())
    }
    val fromRevision = calcFromRevision()
    if (fromRevision.id == UNKNOWN_REVISION_ID) None else fromRevision.findRevisionEntry(entry)
  }

  def content(): String = RevisionEntry.content(id)

  def addCommentary(lineNumber: Option[LineNumberType], comment: String, author: Author): Commentary = Commentary.create(this, lineNumber, comment, author, new java.util.Date())

  def addIssue(lineNumber: Option[LineNumberType], comment: String, author: Author): Issue = Issue.create(this, lineNumber, comment, author, new java.util.Date())

  def feedback(): Traversable[Feedback] = RevisionEntry.feedback(this)
}

object RevisionEntry {
  def find(revisionEntryID: models.RevisionEntryID): Option[RevisionEntry] = inTransaction {
    DBRevisionEntry.lookup(revisionEntryID) match {
      case Some(dbRevisionEntry) => Some(RevisionEntry.dbToModel(Repo.find(dbRevisionEntry.repoID).get, dbRevisionEntry))
      case None => None
    }
  }

  def get(revisionEntryID: models.RevisionEntryID): RevisionEntry = find(revisionEntryID).get

  def feedback(revisionEntry: RevisionEntry): Traversable[Feedback] = inTransaction {
    DBRevisionEntryFeedback.directRevisionEntryFeedback(revisionEntry.id).map {
      e =>
        if (e.feedbackType == DBRevisionEntryFeedbackType.Commentary)
          Commentary(e.id, e.logMessage, Author.get(e.authorID), e.date, revisionEntry, e.lineNumber)
        else
          Issue(e.id, e.logMessage, Author.get(e.authorID), e.date, revisionEntry, e.lineNumber, Feedback.dbToModel(e.status))
    }
  }

  def dbToModel(repo: Repo, dbRevisionEntry: DBRevisionEntry): RevisionEntry = {
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

  def content(revisionEntryID: RevisionEntryID): String = inTransaction {
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
}

case class AddEntry(id: RevisionEntryID, entry: Entry) extends RevisionEntry {
  def operation = "Add"
}

case class DeleteEntry(id: RevisionEntryID, entry: Entry) extends RevisionEntry {
  def operation = "Delete"
}

case class ModifiedEntry(id: RevisionEntryID, entry: Entry) extends RevisionEntry {
  def operation = "Modify"
}

case class ReplacedEntry(id: RevisionEntryID, entry: Entry, copyPath: String, copyRevision: RevisionNumber) extends RevisionEntry {
  def operation = "Replace"

  override def toString: String = operation + ": (" + entry + ", " + copyPath + ", " + copyRevision + ")"
}

object NullRevisionEntry extends RevisionEntry {
  val id: RevisionEntryID = UNKNOWN_REVISION_ENTRY_ID
  val entry: Entry = NullEntry

  def operation = "Unknown"

  override def revisionNumber = -1

  override def previousRevisionEntry(): Option[RevisionEntry] = None

  override def content() = ""
}

trait Review {
}

object Review {
  def apply(dbRevision: DBRevision): Review = dbRevision.reviewStatus match {
    case DBReviewStatus.Complete => CompletedReview(Author.get(dbRevision.reviewAuthorID.get))
    case DBReviewStatus.InProgress => ReviewInProgress(Author.get(dbRevision.reviewAuthorID.get))
    case DBReviewStatus.Outstanding => ReviewOutstanding()
  }
}

case class ReviewOutstanding() extends Review {
  override def toString = "Outstanding"
}

case class ReviewInProgress(author: Author) extends Review {
  override def toString = "In progress"
}

case class CompletedReview(author: Author) extends Review {
  override def toString = "Completed"
}

class Revision(val id: RevisionID, val repo: Repo, val revisionNumber: RevisionNumber, val repoAuthor: Option[RepoAuthor], val date: Date, val logMessage: String, val revisionEntries: Traversable[RevisionEntry], val review: Review) {
  def findRevisionEntry(entry: Entry): Option[RevisionEntry] = {
    revisionEntries.find(p => p.entry.path == entry.path)
  }

  override def toString: String = {
    (id, repo, revisionNumber, repoAuthor, date, logMessage, revisionEntries).toString()
  }
}

object Revision {
  def apply(id: RevisionID, repo: Repo, revisionNumber: RevisionNumber, repoAuthor: Option[RepoAuthor], timestamp: Date, logMessage: String, revisionEntries: Traversable[RevisionEntry], review: Review): Revision = {
    new Revision(id, repo, revisionNumber, repoAuthor, timestamp, logMessage, revisionEntries, review)
  }

  def apply(): Revision = {
    new Revision(UNKNOWN_REVISION_ID, null, UNKNOWN_REVISION_NUMBER, None, null, null, null, ReviewOutstanding())
  }

  def find(revisionID: RevisionID): Option[Revision] = inTransaction {
    val dbRevision = DBRevision.get(revisionID)
    Repo.find(dbRevision.repoID) match {
      case Some(repo) => Some(dbToModel(repo, dbRevision, dbRevision.entries()))
      case None => None
    }
  }

  def get(revisionID: RevisionID): Revision = find(revisionID).get

  def findRevisionOnRevisionEntryID(revisionEntryID: RevisionEntryID): Option[Revision] = inTransaction {
    DBRevisionEntry.lookup(revisionEntryID) match {
      case Some(dbRevisionEntry) => find(dbRevisionEntry.revisionID)
      case None => None
    }
  }

  def dbToModel(repo: Repo, dbRevision: DBRevision, dbRevisionEntries: Iterable[DBRevisionEntry]): Revision =
    new Revision(
      dbRevision.id,
      repo,
      dbRevision.revisionNumber,
      if (dbRevision.repoAuthorID.isDefined) Some(RepoAuthor.get(dbRevision.repoAuthorID.get)) else None,
      dbRevision.date,
      dbRevision.logMessage,
      dbRevisionEntries.map(dbRevisionEntry => RevisionEntry.dbToModel(repo, dbRevisionEntry)),
      Review(dbRevision)
    )
}
