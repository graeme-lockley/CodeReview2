package models

import adaptors.SQLRepository
import java.util.Date

trait Repository {
    def findAuthor(authorID: AuthorID): Option[Author]

    def findAuthorOnName(name: String): Option[Author]

    def findAllRepos(): Iterable[Repo]

    def findRepo(repoID: RepoID): Option[Repo]

    def findRevision(revisionID: RevisionID): Option[Revision]

    def findRevisionOnRevisionEntryID(revisionEntryID: RevisionEntryID): Option[Revision]

    def findRevisionEntry(revisionEntryID: RevisionEntryID): Option[RevisionEntry]

    def revisionEntryFeedback(entry: RevisionEntry): Traversable[Feedback]

    def findCommentary(commentID: CommentID): Option[Commentary]

    def findIssue(issueID: IssueID): Option[Issue]

    def getFileRevision(revisionEntryID: RevisionEntryID): String

    def refreshVCS(repo: Repo): Unit

    def repoRevisions(repo: Repo): Traversable[Revision]

	def repoAuthors(repo: Repo): Traversable[RepoAuthor]

    def entryRevisions(repo: Repo, path: String): Traversable[Revision]

    def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary

    def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse]

    def createCommentaryResponse(commentary: models.Commentary, comment: String, author: models.Author, date: Date): models.CommentaryResponse

    def createIssue(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Issue

    def issueResponses(issue: Issue): Traversable[IssueResponse]

    def updateIssue(issue: Issue): Issue

    def createIssueResponse(issue: Issue, comment: String, author: Author, date: Date): IssueResponse
}

object Repository {
	var repository: Repository = new SQLRepository()

    def findAuthor(authorID: AuthorID): Option[Author] = repository.findAuthor(authorID)

    def findAuthorOnName(name: String): Option[Author] = repository.findAuthorOnName(name)

    def findAllRepos(): Iterable[Repo] = repository.findAllRepos()

    def findRepo(repoID: RepoID): Option[Repo] = repository.findRepo(repoID)

    def findRevision(revisionID: RevisionID): Option[Revision] = repository.findRevision(revisionID)

    def findRevisionOnRevisionEntryID(revisionEntryID: RevisionEntryID): Option[Revision] = repository.findRevisionOnRevisionEntryID(revisionEntryID)

    def findRevisionEntry(revisionEntryID: RevisionEntryID): Option[RevisionEntry] = repository.findRevisionEntry(revisionEntryID)

    def revisionEntryFeedback(entry: RevisionEntry): Traversable[Feedback] = repository.revisionEntryFeedback(entry)

    def findCommentary(commentID: CommentID): Option[Commentary] = repository.findCommentary(commentID)

    def findIssue(issueID: IssueID): Option[Issue] = repository.findIssue(issueID)

    def getFileRevision(revisionEntryID: RevisionEntryID): String = repository.getFileRevision(revisionEntryID)

    def refreshVCS(repo: Repo): Unit = repository.refreshVCS(repo)

    def repoRevisions(repo: Repo): Traversable[Revision] = repository.repoRevisions(repo)

	def repoAuthors(repo: Repo): Traversable[RepoAuthor] = repository.repoAuthors(repo)

    def entryRevisions(repo: Repo, path: String): Traversable[Revision] = repository.entryRevisions(repo, path)

    def createCommentary(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Commentary = repository.createCommentary(revisionEntry, lineNumber, comment, author, date)

    def commentaryResponses(commentary: Commentary): Traversable[CommentaryResponse] = repository.commentaryResponses(commentary)

    def createCommentaryResponse(commentary: Commentary, comment: String, author: Author, date: Date): CommentaryResponse = repository.createCommentaryResponse(commentary, comment, author, date)

    def createIssue(revisionEntry: RevisionEntry, lineNumber: Option[LineNumberType], comment: String, author: Author, date: Date): Issue = repository.createIssue(revisionEntry, lineNumber, comment, author, date)

    def issueResponses(issue: Issue): Traversable[IssueResponse] = repository.issueResponses(issue)

    def updateIssue(issue: Issue): Issue = repository.updateIssue(issue)

    def createIssueResponse(issue: Issue, comment: String, author: Author, date: Date): IssueResponse = repository.createIssueResponse(issue, comment, author, date)
}