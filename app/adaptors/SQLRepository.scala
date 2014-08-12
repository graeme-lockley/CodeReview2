package adaptors

import models._
import org.squeryl.PrimitiveTypeMode._
import ports._

class SQLRepository extends Repository {
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
}
