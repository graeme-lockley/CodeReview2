package controllers

import play.api.mvc.{Action, Controller}
import models.{Author, Repository}
import java.text.SimpleDateFormat

object CommentaryFeedback extends Controller {
    def create() = Action(parse.json) {
        request =>
            val jsValue = request.body

            val comment = (jsValue \ "comment").as[String]
            val authorID = (jsValue \ "authorID").as[Long]
            val commentID = (jsValue \ "commentID").as[Long]

            val commentary = Repository.findCommentary(commentID).get
            val author = Author.find(authorID).get
            val commentaryResponse = commentary.addResponse(comment, author)

            val date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(commentaryResponse.date)
            val jsonResponse = "{\"commentaryResponseID\": " + commentaryResponse.id + ", \"when\": \"" + date + "\"}"

            Ok(jsonResponse)
    }
}
