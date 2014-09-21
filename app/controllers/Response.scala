package controllers

import models.Author
import play.api.mvc.{Action, Controller}

object Response extends Controller {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val issue = models.Feedback.find((jsValue \ "feedbackID").as[Long]).get
      val author = Author.find((jsValue \ "authorID").as[Long]).get

      val response = issue.addResponse((jsValue \ "comment").as[String], author)

      Ok(responseWriter.write(response))
  }
}
