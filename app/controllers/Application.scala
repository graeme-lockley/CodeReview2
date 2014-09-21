package controllers

import play.api.mvc._

object Application extends Controller {
  def index() = Action {
    implicit request =>
      Ok(views.html.portlets.show())
//      Ok(views.html.index("Hello"))
  }
}