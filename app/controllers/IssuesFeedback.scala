package controllers

import play.api.mvc.{Action, Controller}
import models.{Author, Repository}
import play.api.libs.json.Json

object IssuesFeedback extends Controller {
    def create() = Action(parse.json) {
        request =>
            val jsValue = request.body

            val issue = Repository.findIssue((jsValue \ "issueID").as[Long]).get
            val author = Author.find((jsValue \ "authorID").as[Long]).get

            val issueResponse = issue.addResponse((jsValue \ "comment").as[String], author)

            val jsonResponse = Json.obj(
                "issueResponseID" -> issueResponse.id,
                "when" -> dateWriter.write(issueResponse.date)
            )

            Ok(Json.stringify(jsonResponse))
    }
}
