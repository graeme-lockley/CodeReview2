package controllers

import java.text.SimpleDateFormat

import models.{Author, RevisionEntry}
import play.api.mvc.{Action, Controller}

object Commentary extends Controller {
    def create() = Action(parse.json) {
        request =>
            val jsValue = request.body

            val comment = (jsValue \ "comment").as[String]
            val authorID = (jsValue \ "authorID").as[Long]
            val revisionEntryID = (jsValue \ "revisionEntryID").as[Long]
            val lineNumber = (jsValue \ "lineNumber").asOpt[Long]

          val revisionEntry = RevisionEntry.find(revisionEntryID).get
            val author = Author.find(authorID).get
            val commentary = revisionEntry.addCommentary(lineNumber, comment, author)

            val date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(commentary.date)
            val jsonResponse = "{\"commentaryID\": " + commentary.id + ", \"when\": \"" + date + "\"}"

            Ok(jsonResponse)
    }
}
