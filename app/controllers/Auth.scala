package controllers

import models.Author
import org.apache.commons.lang3.math.NumberUtils
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._


object Auth extends Controller {
  def user = Action {
    implicit request =>
      loggedOnUser match {
        case Some(author) => Ok(authorWriter.write(author))
        case None => Ok("{}")
      }
  }

  def loggedOnUser(implicit request: RequestHeader): Option[Author] = {
    val authorId = NumberUtils.toLong(request.session.get(Security.username).getOrElse("-1"), -1)

    Author.find(authorId)
  }

  def login() = Action {
    implicit request =>
      val loginForm = Form(
        single(
          "name" -> nonEmptyText
        )
      )
      val user = loginForm.bindFromRequest().get

      Author.findOnName(user) match {
        case Some(author) =>
          Redirect(routes.Repos.list)
            .withSession(Security.username -> s"${author.id}")
            .flashing("success" -> s"You have successfully logged on - welcome back ${author.name}")
        case None =>
          Redirect(routes.Repos.list)
            .withSession()
            .flashing("error" -> "Unable to login with that user name/password combination.")
      }
  }

  def logout() = Action {
    implicit request =>
      val author = loggedOnUser

      Redirect(routes.Repos.list)
        .withSession()
        .flashing("info" -> s"You have successfully been logged out - cheerio ${if (author.isEmpty) "" else author.get.name}")
  }
}
