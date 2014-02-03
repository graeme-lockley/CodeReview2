package controllers

import play.api.mvc.{Action, Controller}
import models.Repository
import play.api.libs.json._

object Issues extends Controller {
    def create() = Action(parse.json) {
        request =>
            val jsValue = request.body

            val comment = (jsValue \ "comment").as[String]
            val authorID = (jsValue \ "authorID").as[Long]
            val revisionEntryID = (jsValue \ "revisionEntryID").as[Long]
            val lineNumber = (jsValue \ "lineNumber").asOpt[Long]

            val revisionEntry = Repository.findRevisionEntry(revisionEntryID).get
            val author = Repository.findAuthor(authorID).get
            val issue = revisionEntry.addIssue(lineNumber, comment, author)

            val jsonResponse = Json.obj(
                "issueID" -> issue.id,
                "when" -> dateWriter.write(issue.date)
            )

            Ok(Json.stringify(jsonResponse))
    }

    def close(issueID: Long, authorID: Long) = Action(parse.json) {
        request =>
            (Repository.findAuthor(authorID), Repository.findIssue(issueID)) match {
                case (Some(author), Some(issue)) =>
                    issue.close(author) match {
                        case Left(errorMessage) => BadRequest("{\"message\": \"" + errorMessage + "\"}")
                        case Right(updatedIssue) => Ok("{}")
                    }
                case (Some(_), None) => BadRequest("{\"message\": \"Unknown issue\"}")
                case (None, Some(_)) => BadRequest("{\"message\": \"Unknown author\"}")
                case (None, None) => BadRequest("{\"message\": \"Unknown issue and unknown author\"}")
            }
    }

    def show(issueID: Long) = Action {
        request =>
            val issue = Repository.findIssue(issueID).get

            Ok(issueWriter.write(issue))
    }
}


