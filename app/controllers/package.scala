import java.text.SimpleDateFormat
import java.util.Date

import models.{Issue, IssueResponse, _}
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
  }

  val revisionEntryWriter = new {
    def write(revisionEntry: RevisionEntry): JsObject = Json.obj(
      "feedback" -> revisionEntry.feedback().map {
        case c: Commentary => commentaryWriter.write(c)
        case i: Issue => issueWriter.write(i)
      })
  }

  val commentaryWriter = new {
    def write(commentary: Commentary): JsObject = Json.obj(
      "type" -> "comment",
      "id" -> commentary.id,
      "comment" -> commentary.comment,
      "author" -> authorWriter.write(commentary.author),
      "date" -> dateWriter.write(commentary.date),
      "responses" -> commentary.responses().map(x => commentaryResponseWriter.write(x)),
      "lineNumber" -> commentary.lineNumber
    )
  }

  val commentaryResponseWriter = new {
    def write(commentaryResponse: CommentaryResponse): JsObject = Json.obj(
      "id" -> commentaryResponse.id,
      "comment" -> commentaryResponse.comment,
      "author" -> authorWriter.write(commentaryResponse.author),
      "date" -> dateWriter.write(commentaryResponse.date)
    )
  }

  val issueWriter = new {
    def write(issue: Issue): JsObject = Json.obj(
      "type" -> "issue",
      "id" -> issue.id,
      "comment" -> issue.comment,
      "author" -> authorWriter.write(issue.author),
      "date" -> dateWriter.write(issue.date),
      "responses" -> issue.responses().map(x => issueResponseWriter.write(x)),
      "lineNumber" -> issue.lineNumber,
      "status" -> issueStatusWriter.write(issue.status)
    )
  }

  val issueResponseWriter = new {
    def write(issueResponse: IssueResponse): JsObject = Json.obj(
      "id" -> issueResponse.id,
      "comment" -> issueResponse.comment,
      "author" -> authorWriter.write(issueResponse.author),
      "date" -> dateWriter.write(issueResponse.date)
    )
  }

  val repoWriter = new {
    def write(repo: Repo): JsObject = Json.obj(
      "id" -> repo.id,
      "name" -> repo.name,
      "credentials" -> write(repo.credentials)
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
    def write(issueStatus: IssueStatus): JsString = issueStatus match {
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