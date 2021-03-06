package ports

import java.sql.Timestamp
import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean
import org.squeryl.{KeyedEntity, Query, Schema}
import ports.DBEntryType.DBEntryType
import ports.DBResourceType.DBResourceType
import ports.DBReviewStatus.DBReviewStatus
import ports.DBRevisionEntryFeedbackStatus.DBRevisionEntryFeedbackStatus
import ports.DBRevisionEntryFeedbackType.DBRevisionEntryFeedbackType

class DBRepo(val id: Long,
             val name: String,
             val svnURL: String,
             val svnUser: String,
             val svnPassword: String) extends KeyedEntity[Long] {
  def this() = this(0, "", "", "", "")

  def largestRevisionNumber(): Long = {
    val calculateLargestRevisionNumber = from(Library.revisions)(r =>
      where(r.repoID === id)
        compute max(r.revisionNumber)).head.measures
    calculateLargestRevisionNumber getOrElse -1
  }

  def revisions(): Query[(DBRevision, DBRevisionEntry)] = Library.repoRevisions(id)

  def entryRevisions(path: String): Query[(DBRevision, DBRevisionEntry)] = {
    from(Library.revisions, Library.revisionEntries)((r, re) =>
      where(r.repoID === id and r.id === re.revisionID and re.path === path)
        select(r, re)
        orderBy r.date
    )
  }
}

object DBRepo {
  def all(): Query[DBRepo] = from(Library.repos)(w => select(w))

  def lookup(repoID: Long): Option[DBRepo] = {
    Library.repos.lookup(repoID)
  }

  def get(repoID: Long): DBRepo = lookup(repoID).get

  def repoAuthors(repoID: models.RepoID): Traversable[DBRepoAuthor] = {
    from(Library.repoAuthors)(ra =>
      where(ra.repoID === repoID)
        select ra
    )
  }
}

class DBAuthor(val id: Long,
               val name: String,
               val emailAddress: Option[String],
               val isAdmin: Boolean) extends KeyedEntity[Long] {
  def this() = this(0, "", None, false)
}

object DBAuthor {
  def lookup(authorID: Long): Option[DBAuthor] = Library.authors.lookup(authorID)

  def lookup(name: String): Option[DBAuthor] = from(Library.authors)(a =>
    where(a.name === name)
      select a
  ).headOption

  def all(): Traversable[DBAuthor] = from(Library.authors)(w => select(w))
}

class DBRepoAuthor(val id: Long,
                   val repoID: Long,
                   val authorID: Option[Long],
                   val repoAuthorName: String) extends KeyedEntity[Long] {
  def this() = this(0, 0, None, "")
}

object DBRepoAuthor {
  def apply(id: Long, repoID: Long, authorID: Option[Long], repoAuthorName: String): DBRepoAuthor = new DBRepoAuthor(id, repoID, authorID, repoAuthorName)

  def lookup(repoAuthorID: Long): Option[DBRepoAuthor] = Library.repoAuthors.lookup(repoAuthorID)

  def getOrCreate(repoID: Long, repoAuthorName: String): DBRepoAuthor = {
    val possibleResult = from(Library.repoAuthors)(ra =>
      where(ra.repoID === repoID and ra.repoAuthorName === repoAuthorName)
        select ra
    )
    if (possibleResult.size == 0)
      Library.repoAuthors.insert(new DBRepoAuthor(0, repoID, None, repoAuthorName))
    else
      possibleResult.head
  }

  def update(value: DBRepoAuthor) = Library.repoAuthors.update(value)
}

object DBReviewStatus extends Enumeration {
  type DBReviewStatus = Value
  val Outstanding = Value(0, "Outstanding")
  val InProgress = Value(1, "In progress")
  val Complete = Value(2, "Complete")
}

class DBRevision(val id: Long,
                 val repoID: Long,
                 val revisionNumber: Long, // TODO This should be an int rather than a long
                 val repoAuthorID: Option[Long],
                 val date: Timestamp,
                 val logMessage: String,
                 val reviewStatus: DBReviewStatus,
                 val reviewAuthorID: Option[Long]) extends KeyedEntity[Long] {
  def this() = this(0, 0, 0, None, new Timestamp(java.lang.System.currentTimeMillis()), "", DBReviewStatus.Outstanding, None)
}

object DBRevision {
  def apply(repoID: Long, revisionNumber: Long, repoAuthorID: Option[Long], time: Long, logMessage: String) = new DBRevision(-1L, repoID, revisionNumber, repoAuthorID, new java.sql.Timestamp(time), logMessage, DBReviewStatus.Outstanding, None)

  def apply(id: Long, repoID: Long, revisionNumber: Long, repoAuthorID: Option[Long], time: Long, logMessage: String, reviewStatus: DBReviewStatus, reviewAuthorID: Option[Long]) =
    new DBRevision(id, repoID, revisionNumber, repoAuthorID, new java.sql.Timestamp(time), logMessage, reviewStatus, reviewAuthorID)

  def all(): Query[DBRevision] = from(Library.revisions)(w => select(w))

  def all(filter: DBRevision => BinaryOperatorNodeLogicalBoolean): Query[DBRevision] =
    from(Library.revisions)(r => where(filter(r)) select r)

  def lookup(revisionID: Long): Option[DBRevision] = Library.revisions.lookup(revisionID)

  def get(revisionID: Long): DBRevision = lookup(revisionID).get

  def update(value: DBRevision) = Library.revisions.update(value)
}

object DBResourceType extends Enumeration {
  type DBResourceType = Value
  val NoneResource = Value(1, "None")
  val FileResource = Value(2, "File")
  val DirResource = Value(3, "Dir")
  val UnknownResource = Value(4, "Unknown")
}

object DBEntryType extends Enumeration {
  type DBEntryType = Value
  val AddEntry = Value(1, "Add")
  val DeleteEntry = Value(2, "Delete")
  val ModifyEntry = Value(3, "Modify")
  val ReplaceEntry = Value(4, "Replace")
}

class DBRevisionEntry(val id: Long,
                      val repoID: Long,
                      val revisionID: Long,
                      val entryType: DBEntryType,
                      val resourceType: DBResourceType,
                      val path: String,
                      val copyPath: Option[String],
                      val copyRevision: Option[Long]) extends KeyedEntity[Long] {
  def this() = this(0, 0, 0, DBEntryType.AddEntry, DBResourceType.NoneResource, "", None, None)
}

object DBRevisionEntry {
  def lookup(id: Long): Option[DBRevisionEntry] = Library.revisionEntries.lookup(id)

  def get(id: Long): DBRevisionEntry = lookup(id).get

  def find(repoID: Long, revisionID: Long): Iterable[DBRevisionEntry] = {
    from(Library.revisionEntries)(re =>
      where(re.repoID === repoID and re.revisionID === revisionID)
        select re
        orderBy re.path
    )
  }
}

class DBRevisionEntryContent(val id: Long,
                             val content: String) {
  def this() = this(0, "")
}

object DBRevisionEntryContent {
  def lookup(id: Long): Option[DBRevisionEntryContent] =
    from(Library.revisionEntriesContent)(rec => where(rec.id === id) select rec).headOption
}

object DBRevisionEntryFeedbackType extends Enumeration {
  type DBRevisionEntryFeedbackType = Value
  val Commentary = Value(1, "Commentary")
  val CommentaryResponse = Value(2, "Commentary Response")
  val Issue = Value(3, "Issue")
  val IssueResponse = Value(4, "Issue Response")
}

object DBRevisionEntryFeedbackStatus extends Enumeration {
  type DBRevisionEntryFeedbackStatus = Value
  val Open = Value(1, "Open")
  val Closed = Value(2, "Closed")
}

class DBRevisionEntryFeedback(val id: Long,
                              val parentID: Option[Long],
                              val authorID: Long,
                              val revisionEntryID: Long,
                              val lineNumber: Option[Long],
                              val logMessage: String,
                              val date: Timestamp,
                              val feedbackType: DBRevisionEntryFeedbackType,
                              val status: DBRevisionEntryFeedbackStatus) extends KeyedEntity[Long] {
  def this() = this(0, None, 0, 0, None, "", new Timestamp(java.lang.System.currentTimeMillis()), DBRevisionEntryFeedbackType.Commentary, DBRevisionEntryFeedbackStatus.Closed)
}

object DBRevisionEntryFeedback {
  def all(filter: (DBRevisionEntryFeedback) => BinaryOperatorNodeLogicalBoolean) =
    from(Library.revisionEntryFeedback)(r => where(filter(r)) select r)

  def childrenByDate(parentID: Long): Traversable[DBRevisionEntryFeedback] =
    from(Library.revisionEntryFeedback)(re =>
      where(re.parentID === parentID) select re
        orderBy re.date
    )

  def directRevisionEntryFeedback(revisionEntryID: Long): Traversable[DBRevisionEntryFeedback] =
    from(Library.revisionEntryFeedback)(re =>
      where((re.parentID isNull) and (re.revisionEntryID === revisionEntryID))
        select re
        orderBy re.lineNumber
    )
}

class DBEvent(val id: Long, val authorID: Long, val when: Timestamp, val name: String, val content: String) extends KeyedEntity[Long] {
  def this() = this(0, 0, new Timestamp(java.lang.System.currentTimeMillis()), "", "")
}

object DBEvent {
  def apply(id: Long, authorID: Long, when: Date, name: String, content: String) = new DBEvent(id, authorID, new Timestamp(when.getTime), name, content)

  def all(): Query[DBEvent] = from(Library.events)(w => select(w) orderBy(w.id desc))
}

object Library extends Schema {
  val repos = table[DBRepo]("REPOS")
  val authors = table[DBAuthor]("AUTHORS")
  val repoAuthors = table[DBRepoAuthor]("REPO_AUTHORS")
  val revisions = table[DBRevision]("REVISIONS")
  val revisionEntries = table[DBRevisionEntry]("REVISION_ENTRIES")
  val revisionEntriesContent = table[DBRevisionEntryContent]("REVISION_ENTRIES_CONTENT")
  val revisionEntryFeedback = table[DBRevisionEntryFeedback]("REVISION_ENTRY_FEEDBACK")
  val events = table[DBEvent]("EVENTS")

  on(repos)(repo => declare(
    repo.id is autoIncremented
  ))
  on(authors)(author => declare(
    author.id is autoIncremented
  ))
  on(repoAuthors)(repoAuthor => declare(
    columns(repoAuthor.repoID, repoAuthor.repoAuthorName) are(unique, indexed)
  ))
  on(revisions)(revision => declare(
    revision.revisionNumber is indexed,
    revision.logMessage is dbType("varchar(2048)"),
    columns(revision.repoID, revision.revisionNumber) are(unique, indexed)
  ))
  on(revisionEntries)(revisionEntry => declare(
    revisionEntry.path is dbType("varchar(512)"),
    revisionEntry.copyPath is dbType("varchar(512)"),
    columns(revisionEntry.repoID, revisionEntry.revisionID) are indexed,
    columns(revisionEntry.repoID, revisionEntry.path) are indexed
  ))
  on(events)(event => declare(
    event.id is autoIncremented
  ))

  def repoRevisions(repoID: Long): Query[(DBRevision, DBRevisionEntry)] = {
    from(revisions, revisionEntries)((r, re) =>
      where(r.repoID === repoID and r.id === re.revisionID)
        select(r, re)
        orderBy r.date
    )
  }

  def default_author: DBAuthor = authors.lookup(1L).get
}


