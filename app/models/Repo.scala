package models


class Repo(val id: RepoID, val name: String, val credentials: RepoCredentials) {

    def refreshSVN() = Repository.refreshVCS(this)

    def revisions(): Traversable[Revision] = Repository.repoRevisions(this)

    def entryRevisions(path: String): Traversable[Revision] = Repository.entryRevisions(this, path)
}

object NullRepo extends Repo(UNKNOWN_REPO_ID, "NullRepo", NullRepoCredentials) {
    override def refreshSVN() = {}
    override def revisions() = List()
}

object Repo {
    def apply(id: RepoID, name: String, credentials: RepoCredentials): Repo = new Repo(id, name, credentials)
}

case class RepoCredentials(userName: String, password: String, url: String)

object NullRepoCredentials extends RepoCredentials("NullUser", "NullPassword", "NullURL")
