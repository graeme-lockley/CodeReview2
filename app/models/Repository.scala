package models

import adaptors.SQLRepository

trait Repository {
  def getFileRevision(revisionEntryID: RevisionEntryID): String

  def refreshVCS(repo: Repo): Unit
}

object Repository {
  var repository: Repository = new SQLRepository()

  def getFileRevision(revisionEntryID: RevisionEntryID): String = repository.getFileRevision(revisionEntryID)

  def refreshVCS(repo: Repo): Unit = repository.refreshVCS(repo)
}