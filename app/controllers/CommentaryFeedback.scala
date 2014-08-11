package controllers

import java.text.SimpleDateFormat

import models.Author
import play.api.mvc.{Action, Controller}

object CommentaryFeedback extends Controller {
  def create() = Action(parse.json) {
    request =>
      val jsValue = request.body

      val comment = (jsValue \ "comment").as[String]
      val authorID = (jsValue \ "authorID").as[Long]
      val commentID = (jsValue \ "commentID").as[Long]

      val commentary = models.Commentary.find(commentID).get
      val author = Author.find(authorID).get
      val commentaryResponse = commentary.addResponse(comment, author)

      val date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(commentaryResponse.date)
      val jsonResponse = "{\"commentaryResponseID\": " + commentaryResponse.id + ", \"when\": \"" + date + "\"}"

      Ok(jsonResponse)
  }
}
