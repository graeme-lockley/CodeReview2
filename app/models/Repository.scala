package models

import java.util.Date

import adaptors.SQLRepository

trait Repository {
  def getFileRevision(revisionEntryID: RevisionEntryID): String

  def refreshVCS(repo: Repo): Unit

  def entryRevisions(repo: Repo, path: String): Traversable[Revision]

  def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary

  def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse]

  def createCommentaryResponse(commentary: models.Commentary, comment: String, author: models.Author, date: Date): models.CommentaryResponse

  def issueResponses(issue: Issue): Traversable[IssueResponse]

  def createIssueResponse(issue: Issue, comment: String, author: Author, date: Date): IssueResponse
}

object Repository {
  var repository: Repository = new SQLRepository()

  def getFileRevision(revisionEntryID: RevisionEntryID): String = repository.getFileRevision(revisionEntryID)

  def refreshVCS(repo: Repo): Unit = repository.refreshVCS(repo)

  def entryRevisions(repo: Repo, path: String): Traversable[Revision] = repository.entryRevisions(repo, path)

  def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary = repository.createCommentary(revisionEntry, lineNumber, comment, author, date)

  def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse] = repository.commentaryResponses(commentary)

  def createCommentaryResponse(commentary: Commentary, comment: String, author: Author, date: Date): CommentaryResponse = repository.createCommentaryResponse(commentary, comment, author, date)

  def issueResponses(issue: Issue): Traversable[IssueResponse] = repository.issueResponses(issue)

  def createIssueResponse(issue: Issue, comment: String, author: Author, date: Date): IssueResponse = repository.createIssueResponse(issue, comment, author, date)
}