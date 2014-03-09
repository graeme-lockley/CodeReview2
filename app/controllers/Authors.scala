package controllers

import play.api.mvc.{Action, Controller}
import models.Author

object Authors extends Controller {
	def listAsJSON = Action {
		implicit request =>
			Ok(authorWriter.write(Author.all()))
	}

	def listAsHTML = Action {
		implicit request =>
			Ok(views.html.authors.list(Author.all()))
	}
}
