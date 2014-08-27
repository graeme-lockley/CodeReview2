package controllers

import models.Author
import play.api.mvc.{Action, Controller}

object Authors extends Controller {
  def listAsJSON = Action {
    implicit request =>
      Ok(authorWriter.write(Author.all()))
  }

  def listAsHTML = Action {
    implicit request =>
      Ok(views.html.authors.list(Author.all()))
  }

  def show(id: Long) = Action {
    implicit request =>
      Ok(authorWriter.write(Author.get(id)))
  }
}
