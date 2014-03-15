package adaptors

import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import org.squeryl.Schema
import adaptors.DBEntryType.DBEntryType
import adaptors.DBResourceType.DBResourceType
import adaptors.DBRevisionEntryFeedbackType.DBRevisionEntryFeedbackType
import adaptors.DBRevisionEntryFeedbackStatus.DBRevisionEntryFeedbackStatus

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

	def revisions(): Query[(DBRevision, DBRevisionEntry)] = {
		Library.repoRevisions(id)
	}

	def entryRevisions(path: String): Query[(DBRevision, DBRevisionEntry)] = {
		from(Library.revisions, Library.revisionEntries)((r, re) =>
			where(r.repoID === id and r.id === re.revisionID and re.path === path)
				select(r, re)
				orderBy r.date
		)
	}
}

object DBRepo {
	def all(): Query[DBRepo] = {
		from(Library.repos)(w => select(w))
	}

	def lookup(repoID: Long): Option[DBRepo] = {
		Library.repos.lookup(repoID)
	}

	def get(repoID: Long): DBRepo = {
		lookup(repoID).get
	}

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

	//    def author: DBAuthor = Library.authors.lookup(authorID).get
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

class DBRevision(val id: Long,
                 val repoID: Long,
                 val revisionNumber: Long, // TODO This should be an int rather than a long
                 val repoAuthorID: Option[Long],
                 val date: Timestamp,
                 val logMessage: String) extends KeyedEntity[Long] {
	def this() = this(0, 0, 0, None, new Timestamp(java.lang.System.currentTimeMillis()), "")

	def entries(): Iterable[DBRevisionEntry] = {
		from(Library.revisionEntries)(re =>
			where(re.repoID === repoID and re.revisionID === id)
				select re
				orderBy re.path
		)
	}
}

object DBRevision {
	def lookup(revisionID: Long): Option[DBRevision] = {
		Library.revisions.lookup(revisionID)
	}

	def get(revisionID: Long): DBRevision = {
		lookup(revisionID).get
	}
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
	def lookup(id: Long): Option[DBRevisionEntry] = {
		Library.revisionEntries.lookup(id)
	}

	def get(id: Long): DBRevisionEntry = {
		lookup(id).get
	}
}

class DBRevisionEntryContent(val id: Long,
                             val content: String) {
	def this() = this(0, "")
}

object DBRevisionEntryContent {
	def lookup(id: Long): Option[DBRevisionEntryContent] = {
		from(Library.revisionEntriesContent)(rec => where(rec.id === id) select rec).headOption
	}
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
	def childrenByDate(parentID: Long): Traversable[DBRevisionEntryFeedback] = {
		from(Library.revisionEntryComment)(re =>
			where(re.parentID === parentID)
				select re
				orderBy re.date
		)
	}

	def directRevisionEntryFeedback(revisionEntryID: Long): Traversable[DBRevisionEntryFeedback] = {
		from(Library.revisionEntryComment)(re =>
			where((re.parentID isNull) and (re.revisionEntryID === revisionEntryID))
				select re
				orderBy re.date
		)
	}
}

object Library extends Schema {
	val repos = table[DBRepo]("REPOS")
	val authors = table[DBAuthor]("AUTHORS")
	val repoAuthors = table[DBRepoAuthor]("REPO_AUTHORS")
	val revisions = table[DBRevision]("REVISIONS")
	val revisionEntries = table[DBRevisionEntry]("REVISION_ENTRIES")
	val revisionEntriesContent = table[DBRevisionEntryContent]("REVISION_ENTRIES_CONTENT")
	val revisionEntryComment = table[DBRevisionEntryFeedback]("REVISION_ENTRY_FEEDBACK")

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

	def repoRevisions(repoID: Long): Query[(DBRevision, DBRevisionEntry)] = {
		from(revisions, revisionEntries)((r, re) =>
			where(r.repoID === repoID and r.id === re.revisionID)
				select(r, re)
				orderBy r.date
		)
	}

	def default_author: DBAuthor = authors.lookup(1L).get
}


