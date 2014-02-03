package models

class Author(val id: AuthorID, val name: String) {
    override def toString: String = (id, name).toString()
}

object Author {
    def apply(id: AuthorID, name: String) = new Author(id, name)
}