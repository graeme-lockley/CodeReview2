package controllers

import models.Repo
import play.api.mvc.{Action, Controller}

object Repos extends Controller {
  def list = Action {
    implicit request =>
      val repos = Repo.all()

      Ok(views.html.repos.list(repos))
  }

  def show(id: Long) = Action {
    implicit request =>
      val repo = Repo.get(id)
      Ok(views.html.repos.show(repo))
  }

  def refresh(id: Long) = Action {
    implicit request =>
      val repo = Repo.get(id)
      repo.refreshSVN()
      Ok(views.html.repos.show(repo))
  }

  def authors(id: Long) = Action {
    implicit request =>
      val repo = Repo.find(id).get
      Ok(repoAuthorWriter.write(repo.repoAuthors()))
  }
}