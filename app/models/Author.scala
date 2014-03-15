package models

import org.squeryl.PrimitiveTypeMode._
import adaptors.DBAuthor

class Author(val id: AuthorID, val name: String, val emailAddress: Option[EmailAddress], val isAdmin: Boolean) {
    override def toString: String = (id, name, emailAddress).toString()
}

object Author {
    def apply(id: AuthorID, name: String, emailAddress: Option[EmailAddress], isAdmin: Boolean) = new Author(id, name, emailAddress, isAdmin)

	def all(): Traversable[Author] = inTransaction {
	 	DBAuthor.all().map(x => convertDBAuthorToAuthor(x))
	}

	private def convertDBAuthorToAuthor(dbAuthor: DBAuthor): Author = Author(dbAuthor.id, dbAuthor.name, dbAuthor.emailAddress, dbAuthor.isAdmin)
}