package models

import java.util.Date

sealed trait Entry {
    val repo: Repo
    val path: String

    def revisions(): Traversable[Revision] = Repository.entryRevisions(repo, path)
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
}

sealed trait RevisionEntry {
    val id: RevisionEntryID
    val entry: Entry

    def operation: String

    override def toString: String = operation + ": (" + entry + ")"

    def revisionNumber: RevisionNumber = {
        Repository.findRevisionOnRevisionEntryID(id) match {
            case Some(revision) => revision.revisionNumber
            case None => UNKNOWN_REVISION_NUMBER
        }
    }

    def revision: Revision = Repository.findRevisionOnRevisionEntryID(id).get


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

    def content(): String = Repository.getFileRevision(id)

    def addCommentary(lineNumber: Option[LineNumberType], comment: String, author: Author): Commentary = Repository.createCommentary(this,lineNumber, comment, author, new java.util.Date())
    def addIssue(lineNumber: Option[LineNumberType], comment: String, author: Author): Issue = Repository.createIssue(this,lineNumber, comment, author, new java.util.Date())

    def feedback(): Traversable[Feedback] = Repository.revisionEntryFeedback(this)
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

class Revision(val id: RevisionID, val repo: Repo, val revisionNumber: RevisionNumber, val repoAuthor: Option[RepoAuthor], val date: Date, val logMessage: String, val revisionEntries: Traversable[RevisionEntry]) {
    def findRevisionEntry(entry: Entry): Option[RevisionEntry] = {
        revisionEntries.find(p => p.entry.path == entry.path)
    }

    override def toString: String = {
        (id, repo, revisionNumber, repoAuthor, date, logMessage, revisionEntries).toString()
    }
}

object Revision {
    def apply(id: RevisionID, repo: Repo, revisionNumber: RevisionNumber, repoAuthor: Option[RepoAuthor], timestamp: Date, logMessage: String, revisionEntries: Traversable[RevisionEntry]): Revision = {
        new Revision(id, repo, revisionNumber, repoAuthor, timestamp, logMessage, revisionEntries)
    }

    def apply(): Revision = {
        new Revision(UNKNOWN_REVISION_ID, null, UNKNOWN_REVISION_NUMBER, None, null, null, null)
    }
}
