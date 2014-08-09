package models

import adaptors.DBAuthor
import org.squeryl.PrimitiveTypeMode._

class Author(val id: AuthorID, val name: String, val emailAddress: Option[EmailAddress], val isAdmin: Boolean) {
  override def toString: String = (id, name, emailAddress).toString()
}

object Author {
  def apply(id: AuthorID, name: String, emailAddress: Option[EmailAddress], isAdmin: Boolean) = new Author(id, name, emailAddress, isAdmin)

  def all(): Traversable[Author] = inTransaction {
    DBAuthor.all().map(x => convertDBAuthorToAuthor(x))
  }

  def find(authorID: AuthorID): Option[Author] = inTransaction {
    DBAuthor.lookup(authorID) match {
      case Some(dbAuthor) => Some(convertDBAuthorToAuthor(dbAuthor))
      case None => None
    }
  }

  def findOnName(name: String): Option[Author] = inTransaction {
    DBAuthor.lookup(name) match {
      case Some(dbAuthor) => Some(convertDBAuthorToAuthor(dbAuthor))
      case None => None
    }
  }

  def get(authorID: AuthorID): Author = find(authorID).get

  private def convertDBAuthorToAuthor(dbAuthor: DBAuthor): Author = Author(dbAuthor.id, dbAuthor.name, dbAuthor.emailAddress, dbAuthor.isAdmin)
}