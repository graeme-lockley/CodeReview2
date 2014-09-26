package controllers

import controllers.Revisions._
import play.api.mvc.Action

object Events {
  def list() = Action {
    implicit request =>
      Ok(eventWriter.write(models.Event.all()))
  }
}