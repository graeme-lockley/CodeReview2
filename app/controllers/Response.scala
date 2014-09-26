package controllers

import models.{Author, CreateResponseEvent}
import play.api.mvc.{Action, Controller}

object Response extends Controller {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val issue = models.Feedback.find((jsValue \ "feedbackID").as[Long]).get
      val author = Author.find((jsValue \ "authorID").as[Long]).get

      val comment: String = (jsValue \ "comment").as[String]
      val response = issue.addResponse(comment, author)

      CreateResponseEvent(response.id, issue.id, author.id, response.comment).publish()

      Ok(responseWriter.write(response))
  }
}
