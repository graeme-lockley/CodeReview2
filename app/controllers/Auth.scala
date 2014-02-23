package controllers

import play.api.mvc._
import models.{Repository, Author}
import org.apache.commons.lang3.math.NumberUtils
import play.api.data._
import play.api.data.Forms._


object Auth extends Controller {
    def loggedOnUser(implicit request: RequestHeader): Option[Author] = {
        val authorId = NumberUtils.toLong(request.session.get(Security.username).getOrElse("-1"), -1)

        println(s"AuthorID: $authorId")

        Repository.findAuthor(authorId)
    }

    def login() = Action {
        implicit request =>
            val loginForm = Form(
                tuple(
                    "name" -> nonEmptyText,
                    "password" -> text
                )
            )
            val (user, password) = loginForm.bindFromRequest().get

            println(s"user -> [$user]")
            println(s"password -> [$password]")

            Repository.findAuthorOnName(user) match {
                case Some(author) =>
                    println(s"Logged on: ${author.id}(${author.name})")
                    Redirect(routes.Repos.list).withSession(Security.username -> s"${author.id}")
                case None =>
                    println("Unable to logon... add a flash error...")
                    Ok(views.html.index("Bob 1"))
            }
    }

    def logout() = Action {
        implicit request =>
            Redirect(routes.Repos.list).withSession()
    }
}
