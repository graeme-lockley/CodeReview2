package adaptors

import java.io.ByteArrayOutputStream
import java.util

import models.{AddEntry, DeleteEntry, DirEntry, FileEntry, ModifiedEntry, NoneEntry, ReplacedEntry, UnknownEntry, _}
import org.squeryl.PrimitiveTypeMode._
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.io.{SVNRepository => SVNRepo, SVNRepositoryFactory => SVNRepoFactory}
import org.tmatesoft.svn.core.wc.SVNWCUtil
import ports.DBResourceType.DBResourceType
import ports._

import scala.collection.JavaConverters._

object SVNRepository {
    def refresh(repo: Repo): Unit = inTransaction {
        val repoID = repo.id
        val dbRepo = DBRepo.get(repoID)

        for (repoRevision: (Revision, Traversable[RevisionEntry]) <- repoRevisions(repo, dbRepo.largestRevisionNumber())) {
            val dbRepoAuthorID = repoRevision._1.repoAuthor match {
                case Some(author) => Some(DBRepoAuthor.getOrCreate(author.repo.id, author.name).id)
                case None => None
            }
            val dbRevision = DBRevision(repoID, repoRevision._1.revisionNumber, dbRepoAuthorID, repoRevision._1.date.getTime, repoRevision._1.logMessage)

            Library.revisions.insert(dbRevision)

            for (revisionEntry <- repoRevision._2) {
                val dbRevisionEntry = revisionEntry match {
                    case AddEntry(id, revisionID, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.AddEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case DeleteEntry(id, revisionID, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.DeleteEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case ModifiedEntry(id, revisionID, entry) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.ModifyEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, None, None)
                    case ReplacedEntry(id, revisionID, entry, path, revision) => new DBRevisionEntry(id, repoID, dbRevision.id, DBEntryType.ReplaceEntry, entryToResourceType(revisionEntry.entry), revisionEntry.entry.path, Some(path), Some(revision))
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

    private def repoRevisions(repo: Repo, revisionNumber: Long): Traversable[(Revision, Traversable[RevisionEntry])] = {
        val repository = svnRepository(repo)

        def convertLogEntry(logEntry: SVNLogEntry): (Revision, Traversable[RevisionEntry]) = {
            val revisionEntries = logEntry.getChangedPaths.values.asScala.map(convertLogEntryPath)
            val author = if (logEntry.getAuthor == null) None else Some(RepoAuthor(-1L, repo, None, logEntry.getAuthor))
            System.out.println("convertLogEntry: " + logEntry.getRevision + " " + logEntry.getDate)

            (new Revision(-1L, repo, logEntry.getRevision, author, logEntry.getDate, logEntry.getMessage, ReviewOutstanding()), revisionEntries)
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
                case 'A' => new AddEntry(UNKNOWN_REVISION_ENTRY_ID, UNKNOWN_REVISION_ID, kindOfEntry)
                case 'D' => new DeleteEntry(UNKNOWN_REVISION_ENTRY_ID, UNKNOWN_REVISION_ID, kindOfEntry)
                case 'M' => new ModifiedEntry(UNKNOWN_REVISION_ENTRY_ID, UNKNOWN_REVISION_ID, kindOfEntry)
                case 'R' => new ReplacedEntry(UNKNOWN_REVISION_ENTRY_ID, UNKNOWN_REVISION_ID, kindOfEntry, entryPath.getCopyPath, entryPath.getCopyRevision)
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

    private def svnRepository(repo: Repo): SVNRepo = {
        val repository = SVNRepoFactory.create(SVNURL.parseURIEncoded(repo.credentials.url))
        repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(repo.credentials.userName, repo.credentials.password))
        repository
    }
}
