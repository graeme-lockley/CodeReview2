package object models {
  type AuthorID = Long
  type FeedbackID = Long
  type ResponseID = Long
  type EmailAddress = String
  type LineNumberType = Long
  type RepoID = Long
  type RepoAuthorID = Long
  type RevisionID = Long
  type RevisionEntryID = Long
  type RevisionNumber = Long

  val UNKNOWN_REPO_ID = -1
  val UNKNOWN_REVISION_ID: RevisionEntryID = -1
  val UNKNOWN_REVISION_NUMBER: Long = -1
  val UNKNOWN_REVISION_ENTRY_FEEDBACK_ID: Long = -1
  val INITIAL_REVISION_NUMBER = 0
  val UNKNOWN_REVISION_ENTRY_ID = -1
}

