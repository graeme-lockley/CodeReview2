import java.text.SimpleDateFormat
import java.util.Date

import models.{Issue, IssueResponse, _}
import play.api.libs.json.{JsObject, JsString, Json, _}

package object controllers {
	val revisionEntryWriter = new {
		def write(revisionEntry: RevisionEntry): JsObject = {
			Json.obj(
				"feedback" -> revisionEntry.feedback().map {
					case c: Commentary => commentaryWriter.write(c)
					case i: Issue => issueWriter.write(i)
				})
		}
	}

	val commentaryWriter = new {
		def write(commentary: Commentary): JsObject = {
			Json.obj(
				"type" -> "comment",
				"id" -> commentary.id,
				"comment" -> commentary.comment,
				"author" -> authorWriter.write(commentary.author),
				"date" -> dateWriter.write(commentary.date),
				"responses" -> commentary.responses().map(x => commentaryResponseWriter.write(x)),
				"lineNumber" -> commentary.lineNumber
			)
		}
	}

	val commentaryResponseWriter = new {
		def write(commentaryResponse: CommentaryResponse): JsObject = {
			Json.obj(
				"id" -> commentaryResponse.id,
				"comment" -> commentaryResponse.comment,
				"author" -> authorWriter.write(commentaryResponse.author),
				"date" -> dateWriter.write(commentaryResponse.date)
			)
		}
	}

	val issueWriter = new {
		def write(issue: Issue): JsObject = {
			Json.obj(
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
	}

	val issueResponseWriter = new {
		def write(issueResponse: IssueResponse): JsObject = {
			Json.obj(
				"id" -> issueResponse.id,
				"comment" -> issueResponse.comment,
				"author" -> authorWriter.write(issueResponse.author),
				"date" -> dateWriter.write(issueResponse.date)
			)
		}
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
		def write(author: Author): JsObject = {
			Json.obj(
				"id" -> author.id,
				"name" -> author.name,
				"emailAddress" -> author.emailAddress,
				"isAdmin" -> author.isAdmin
			)
		}

		def write(authors: Traversable[Author]): JsValue = Json.toJson(authors.map(x => write(x)))
	}

	val dateWriter = new {
		def write(date: Date): JsString = {
			JsString(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date))
		}
	}

	val issueStatusWriter = new {
		def write(issueStatus: IssueStatus): JsString = issueStatus match {
			case Open() => JsString("Open")
			case Closed() => JsString("Closed")
		}
	}
}
