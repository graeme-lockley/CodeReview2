package adaptors

import models._
import adaptors.DBResourceType.DBResourceType
import org.squeryl.PrimitiveTypeMode._
import org.tmatesoft.svn.core._
import java.util
import java.io.ByteArrayOutputStream
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import models.UnknownEntry
import models.ReplacedEntry
import models.ModifiedEntry
import models.NoneEntry
import models.DeleteEntry
import scala.Some
import models.DirEntry
import models.FileEntry
import models.AddEntry
import org.tmatesoft.svn.core.wc.SVNWCUtil
import scala.collection.JavaConverters._

object SVNRepository {
    def refresh(repo: Repo): Unit = inTransaction {
        val repoID = repo.id
        val dbRepo = DBRepo.get(repoID)
        for (repoRevision: Revision <- repoRevisions(repo, dbRepo.largestRevisionNumber())) {
            val dbRepoAuthorID = repoRevision.repoAuthor match {
                case Some(author) => Some(DBRepoAuthor.getOrCreate(author.repo.id, author.name).id)
                case None => None
            }
            val dbRevision = new DBRevision(-1L, repoID, repoRevision.revisionNumber, dbRepoAuthorID, new java.sql.Timestamp(repoRevision.date.getTime), repoRevision.logMessage)

            Library.revisions.insert(dbRevision)

            for (revisionEntry <- repoRevision.revisionEntries) {
                val dbRevisionEntry = revisionEntry match {
                    case AddEntry(id, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.AddEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case DeleteEntry(id, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.DeleteEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case ModifiedEntry(id, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.ModifyEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case ReplacedEntry(id, entry, path, revision) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.ReplaceEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, Some(path), Some(revision))
                }
                Library.revisionEntries.insert(dbRevisionEntry)
            }
        }
    }

    private def entryToResourceType(entry: Entry): DBResourceType = entry match {
        case NoneEntry(_, _) => DBResourceType.NoneResource
        case FileEntry(_, _) => DBResourceType.FileResource
        case DirEntry(_, _) => DBResourceType.DirResource
        case UnknownEntry(_, _) => DBResourceType.UnknownResource
    }

    private def repoRevisions(repo: Repo, revisionNumber: Long): Traversable[Revision] = {
        val repository = svnRepository(repo)

        def convertLogEntry(logEntry: SVNLogEntry): Revision = {
            val revisionEntries = logEntry.getChangedPaths.values.asScala.map(convertLogEntryPath)
            val author = if (logEntry.getAuthor == null) None else Some(RepoAuthor(-1L, repo, None, logEntry.getAuthor))
            new Revision(-1, repo, logEntry.getRevision, author, logEntry.getDate, logEntry.getMessage, revisionEntries)
        }

        def convertNodeKind(kind: SVNNodeKind, path: String): Entry = kind match {
            case SVNNodeKind.DIR => new DirEntry(repo, path)
            case SVNNodeKind.FILE => new FileEntry(repo, path)
            case SVNNodeKind.NONE => new NoneEntry(repo, path)
            case SVNNodeKind.UNKNOWN => new UnknownEntry(repo, path)
        }

        def convertLogEntryPath(entryPath: SVNLogEntryPath): RevisionEntry = {
            val kindOfEntry = convertNodeKind(entryPath.getKind, entryPath.getPath)
            entryPath.getType match {
                case 'A' => new AddEntry(UNKNOWN_REVISION_ENTRY_ID, kindOfEntry)
                case 'D' => new DeleteEntry(UNKNOWN_REVISION_ENTRY_ID, kindOfEntry)
                case 'M' => new ModifiedEntry(UNKNOWN_REVISION_ENTRY_ID, kindOfEntry)
                case 'R' => new ReplacedEntry(UNKNOWN_REVISION_ENTRY_ID, kindOfEntry, entryPath.getCopyPath, entryPath.getCopyRevision)
                case _ => throw new IllegalArgumentException("SVNRepo.convert: unknown entryPath type: " + entryPath.getType)
            }
        }
        val endRevision = UNKNOWN_REVISION_ID
        val startRevision = if (revisionNumber == UNKNOWN_REVISION_NUMBER) INITIAL_REVISION_NUMBER else revisionNumber
        val logEntries = repository.log(Array(""), null, startRevision, endRevision, true, true).asInstanceOf[util.Collection[SVNLogEntry]]
        logEntries.asScala.filter(p => p.getRevision != revisionNumber).map((entry: SVNLogEntry) => convertLogEntry(entry))
    }

    def getFileRevision(repo: Repo, fileName: String, revisionNumber: Int): String = {
        val repository = svnRepository(repo)
        val outputStream = new ByteArrayOutputStream()
        val properties = new SVNProperties()

        repository.getFile(fileName, revisionNumber, properties, outputStream)
        outputStream.toString
    }

    private def svnRepository(repo: Repo): SVNRepository = {
        val repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(repo.credentials.url))
        repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(repo.credentials.userName, repo.credentials.password))
        repository
    }
}
