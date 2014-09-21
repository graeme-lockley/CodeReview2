package controllers

import models.Author
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

object Response extends Controller {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val issue = models.Feedback.find((jsValue \ "feedbackID").as[Long]).get
      val author = Author.find((jsValue \ "authorID").as[Long]).get

      val issueResponse = issue.addResponse((jsValue \ "comment").as[String], author)

      val jsonResponse = Json.obj(
        "responseID" -> issueResponse.id,
        "when" -> dateWriter.write(issueResponse.date)
      )

      Ok(Json.stringify(jsonResponse))
  }
}
