package models

class Author(val id: AuthorID, val name: String, val emailAddress: Option[EmailAddress], val isAdmin: Boolean) {
    override def toString: String = (id, name, emailAddress).toString()
}

object Author {
    def apply(id: AuthorID, name: String, emailAddress: Option[EmailAddress], isAdmin: Boolean) = new Author(id, name, emailAddress, isAdmin)
}