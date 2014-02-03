# --- !Ups

insert into Repos (Name, SVNUser, SVNPassword, SVNURL) VALUES ('SVNKit Repo', 'anonymous', 'anonymous', 'http://svn.svnkit.com/repos/svnkit/trunk');

# --- !Downs

delete from Repos where Name = 'SVNKit Repo';
