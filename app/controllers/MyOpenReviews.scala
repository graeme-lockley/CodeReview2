package controllers

import models.Author
import org.squeryl.PrimitiveTypeMode._
import ports.Library

class MyOpenReviews(val me: Author) {
  def items() = inTransaction {
    dbItems().map(r => models.Revision.dbToModel(models.Repo.get(r.repoID), r))
  }

  private def dbItems() =
    from(Library.revisions, Library.revisionEntries, Library.revisionEntryFeedback)((r, re, ref) =>
      where(r.id === re.revisionID and re.id === ref.revisionEntryID and
        ref.parentID.isNull and ref.status.id === 1 and
        ref.authorID === me.id).select(r)).distinct
}

object MyOpenReviews {
  def main(args: Array[String]) {
    val app = new play.core.StaticApplication(new java.io.File("."))
    val session = org.squeryl.Session.create(play.api.db.DB.getConnection()(app.application), new org.squeryl.adapters.H2Adapter)

    new MyOpenReviews(models.Author.get(1)).items() foreach println
  }
}
