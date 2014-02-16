# --- !Ups

CREATE TABLE Repo_Authors (
  id             BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  repoID         BIGINT       NOT NULL,
  authorID       BIGINT,
  repoAuthorName VARCHAR(128) NOT NULL
);

-- foreign key constraints :
ALTER TABLE Repo_Authors ADD CONSTRAINT Repo_Authors_FK1 FOREIGN KEY (repoID) REFERENCES REPOS (id);
ALTER TABLE Repo_Authors ADD CONSTRAINT Repo_Authors_FK2 FOREIGN KEY (authorID) REFERENCES AUTHORS (id);

-- column group indexes :
CREATE UNIQUE INDEX Repo_Authors_IDX1 ON Repo_Authors (repoID, repoAuthorName);


# --- !Downs

DROP TABLE Repo_Authors;
