package controllers

import play.api.mvc.{Action, Controller}
import models.Repository

object Repos extends Controller {
    def list = Action {
        implicit request =>
            val repos = Repository.findAllRepos()

            Ok(views.html.repos.list(repos))
    }

    def show(id: Long) = Action {
        implicit request =>
            val repo = Repository.findRepo(id)
            Ok(views.html.repos.show(repo.get))
    }

    def refresh(id: Long) = Action {
        implicit request =>
            val repo = Repository.findRepo(id)
            repo.get.refreshSVN()
            Ok(views.html.repos.show(repo.get))
    }
}