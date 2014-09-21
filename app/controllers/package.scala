import java.text.SimpleDateFormat
import java.util.Date

import models._
import play.api.libs.json.{JsObject, JsString, Json, _}

package object controllers {
  val revisionWriter = new {
    def write(revision: Revision, verbs: Iterable[Verb]): JsObject = write(revision).+("verbs", verbWriter.write(verbs))

    def write(revision: Revision): JsObject = {
      val result = Json.obj(
        "id" -> revision.id,
        "repo" -> repoWriter.write(revision.repo),
        "number" -> revision.revisionNumber,
        "date" -> dateWriter.write(revision.date),
        "logMessage" -> revision.logMessage,
        "review" -> write(revision.review)
      )
      revision.repoAuthor match {
        case None => result
        case Some(author) => result.+("author", repoAuthorWriter.write(author))
      }
    }

    def write(review: Review): JsObject = review match {
      case ReviewOutstanding() => Json.obj("state" -> "Outstanding")
      case ReviewInProgress(author) => Json.obj("state" -> "In progress", "author" -> authorWriter.write(author))
      case ReviewComplete(author) => Json.obj("state" -> "Complete", "author" -> authorWriter.write(author))
    }

    def write(revisions: Traversable[Revision]): JsValue = Json.toJson(revisions.map(x => write(x)))
  }

  val revisionEntryWriter = new {
    def write(revisionEntry: RevisionEntry): JsObject = Json.obj(
      "id" -> revisionEntry.id,
      "entry" -> write(revisionEntry.entry),
      "operation" -> revisionEntry.operation,
      "feedback" -> revisionEntry.feedback().map { f => feedbackWriter.write(f)}
    )

    def write(revisionEntries: Traversable[RevisionEntry]): JsValue = Json.toJson(revisionEntries.map(revisionEntry => write(revisionEntry)))

    def write(entry: Entry): JsObject = entry match {
      case NoneEntry(repo, path) => Json.obj(
        "type" -> "none",
        "repo" -> repoWriter.writeSummary(repo),
        "path" -> path)
      case FileEntry(repo, path) => Json.obj(
        "type" -> "file",
        "repo" -> repoWriter.writeSummary(repo),
        "path" -> path)
      case DirEntry(repo, path) => Json.obj(
        "type" -> "dir",
        "repo" -> repoWriter.writeSummary(repo),
        "path" -> path)
      case UnknownEntry(repo, path) => Json.obj(
        "type" -> "unknown",
        "repo" -> repoWriter.writeSummary(repo),
        "path" -> path)
    }
  }

  val feedbackWriter = new {
    def write(feedback: Feedback): JsObject = Json.obj(
      "type" -> "issue",
      "id" -> feedback.id,
      "comment" -> feedback.comment,
      "author" -> authorWriter.write(feedback.author),
      "date" -> dateWriter.write(feedback.date),
      "responses" -> feedback.responses().map(x => responseWriter.write(x)),
      "lineNumber" -> feedback.lineNumber,
      "status" -> issueStatusWriter.write(feedback.status)
    )

    def write(feedback: Traversable[Feedback]): JsValue = Json.toJson(feedback.map { r => write(r)})
  }

  val responseWriter = new {
    def write(response: Response): JsObject = Json.obj(
      "id" -> response.id,
      "comment" -> response.comment,
      "author" -> authorWriter.write(response.author),
      "date" -> dateWriter.write(response.date)
    )
  }

  val repoWriter = new {
    def write(repo: Repo): JsObject = Json.obj(
      "id" -> repo.id,
      "name" -> repo.name,
      "credentials" -> write(repo.credentials)
    )

    def writeSummary(repo: Repo): JsObject = Json.obj(
      "id" -> repo.id
    )

    def write(repoCredentials: RepoCredentials): JsObject = Json.obj(
      "username" -> repoCredentials.userName,
      "password" -> repoCredentials.password,
      "url" -> repoCredentials.url
    )
  }

  val repoAuthorWriter = new {
    def write(repoAuthor: RepoAuthor): JsObject = {
      val result = Json.obj(
        "id" -> repoAuthor.id,
        "repoID" -> repoAuthor.repo.id,
        "name" -> repoAuthor.name
      )

      repoAuthor.author match {
        case Some(author) => result ++ Json.obj("author" -> authorWriter.write(author))
        case None => result
      }
    }

    def write(repoAuthors: Traversable[RepoAuthor]): JsValue = {
      Json.toJson(repoAuthors.map(x => write(x)))
    }
  }

  val repoAuthorReader = new {
    def read(json: JsValue): RepoAuthor = {
      val author = (json \ "author" \ "id").asOpt[Long] match {
        case None => None
        case Some(authorID) => Author.find(authorID)
      }
      RepoAuthor(
        (json \ "id").as[Long],
        Repo.get((json \ "repoID").as[Long]),
        author,
        (json \ "name").as[String])
    }
  }

  val authorWriter = new {
    def write(author: Author): JsObject = Json.obj(
      "id" -> author.id,
      "name" -> author.name,
      "emailAddress" -> author.emailAddress,
      "isAdmin" -> author.isAdmin
    )

    def write(authors: Traversable[Author]): JsValue = Json.toJson(authors.map(x => write(x)))
  }

  val dateWriter = new {
    def write(date: Date): JsString = JsString(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date))
  }

  val issueStatusWriter = new {
    def write(issueStatus: FeedbackStatus): JsString = issueStatus match {
      case Open() => JsString("Open")
      case Closed() => JsString("Closed")
    }
  }

  val verbWriter = new {
    def write(verb: Verb): JsObject = Json.obj(
      verb.name -> verb.endPoint.url
    )

    def write(verbs: Traversable[Verb]): JsValue = {
      def write(jsObj: JsObject, verbs: Traversable[Verb]): JsObject = verbs match {
        case v :: vs => write(jsObj.+(v.name, JsString(v.endPoint.url)), vs)
        case Nil => jsObj
      }

      write(Json.obj(), verbs)
    }
  }
}