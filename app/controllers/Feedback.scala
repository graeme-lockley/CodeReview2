package controllers

import models.Revision
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean
import play.api.libs.json.Json
import play.api.mvc.Action
import ports.{DBRevision, DBRevisionEntryFeedback}

object Feedback extends AuthController {
  var queries: Map[String, DBRevision => BinaryOperatorNodeLogicalBoolean] = Map()

  queries = queries

  def list = Action {
    implicit request =>
      val query = request.getQueryString("query")
      val all =
        if (query.getOrElse("").equals("mine"))
          Revision.all((r: DBRevisionEntryFeedback) => r.id === 0 and (r.parentID === 1))
        else if (query.isEmpty)
          Revision.all()
        else
          Revision.all(queries.get(query.get).get)

      Ok(Json.stringify(revisionWriter.write(all)))
  }
}
