import org.squeryl.adapters.H2Adapter
import org.squeryl.{SessionFactory, Session}
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import play.api.db.DB
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {
    override def onStart(app: Application) {
        SessionFactory.concreteFactory = Some(() =>
            Session.create(DB.getConnection()(app), new H2Adapter))

        SVNRepositoryFactoryImpl.setup()
    }
}
