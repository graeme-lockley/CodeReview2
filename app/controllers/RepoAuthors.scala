package controllers

import play.api.mvc.{Action, Controller}

object RepoAuthors extends Controller {
	def update(id: Long) = Action(parse.json) {
		implicit request =>
			val repoAuthor = repoAuthorReader.read(request.body)
			repoAuthor.update()

			Ok("{code: 0}")
	}
}
