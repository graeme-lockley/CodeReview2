package models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import ports.{DBRepo, DBRepoAuthor, DBRevision, DBRevisionEntry}

import scala.collection.mutable

class Repo(val id: RepoID, val name: String, val credentials: RepoCredentials) {

  def refreshSVN() = Repository.refreshVCS(this)

  def revisions(): Traversable[Revision] = Repo.revisions(this)

  def entryRevisions(path: String): Traversable[Revision] = Repository.entryRevisions(this, path)

  def repoAuthors(): Traversable[RepoAuthor] = Repository.repoAuthors(this)
}

object NullRepo extends Repo(UNKNOWN_REPO_ID, "NullRepo", NullRepoCredentials) {
  override def refreshSVN() = {}

  override def revisions() = List()
}

object Repo {
  def apply(id: RepoID, name: String, credentials: RepoCredentials): Repo = new Repo(id, name, credentials)

  def all(): Iterable[Repo] = inTransaction {
    DBRepo.all().map(dbToModel)
  }

  def find(repoID: models.RepoID): Option[Repo] = inTransaction {
    DBRepo.lookup(repoID) match {
      case Some(dbRepo) => Some(dbToModel(dbRepo))
      case None => None
    }
  }

  def get(repoID: models.RepoID): Repo = find(repoID).get

  private def dbToModel(dbRepo: DBRepo): Repo = Repo(dbRepo.id, dbRepo.name, RepoCredentials(dbRepo.svnUser, dbRepo.svnPassword, dbRepo.svnURL))

  def revisions(repo: Repo): Traversable[Revision] = inTransaction {
    val dbRepo = DBRepo.get(repo.id)

    convertToRevisions(repo, dbRepo.revisions())
  }

  def convertToRevisions(repo: Repo, dbQueryResult: Query[(DBRevision, DBRevisionEntry)]): Traversable[Revision] = {
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
      Revision.dbToModel(repo, dbRevision, dbRevisionEntries)
    })
  }
}

case class RepoCredentials(userName: String, password: String, url: String)

object NullRepoCredentials extends RepoCredentials("NullUser", "NullPassword", "NullURL")

class RepoAuthor(val id: RepoAuthorID, val repo: Repo, val author: Option[Author], val name: String) {
  def save() = RepoAuthor.save(this)
}

object RepoAuthor {
  def apply(id: RepoAuthorID, repo: Repo, author: Option[Author], name: String): RepoAuthor = new RepoAuthor(id, repo, author, name)

  def save(repoAuthor: RepoAuthor) = inTransaction {
    DBRepoAuthor.update(modelToDB(repoAuthor))
  }

  def find(repoAuthorID: RepoAuthorID): Option[RepoAuthor] = inTransaction {
    DBRepoAuthor.lookup(repoAuthorID) match {
      case Some(dbRepoAuthor) => Some(dbToModel(dbRepoAuthor))
      case None => None
    }
  }

  def get(repoAuthorID: RepoAuthorID): RepoAuthor = find(repoAuthorID).get

  def dbToModel(dbRepoAuthor: DBRepoAuthor): RepoAuthor = {
    val author = dbRepoAuthor.authorID match {
      case Some(dbAuthorID) => Some(Author.get(dbAuthorID))
      case None => None
    }
    RepoAuthor(dbRepoAuthor.id, Repo.find(dbRepoAuthor.repoID).get, author, dbRepoAuthor.repoAuthorName)
  }

  def modelToDB(repoAuthor: RepoAuthor): DBRepoAuthor = DBRepoAuthor(repoAuthor.id, repoAuthor.repo.id, repoAuthor.author.map(x => x.id), repoAuthor.name)
}