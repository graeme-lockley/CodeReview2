# --- !Ups

CREATE TABLE DBRepoAuthor (
  authorID       BIGINT       NOT NULL,
  repoID         BIGINT       NOT NULL,
  repoAuthorName VARCHAR(128) NOT NULL
);

-- foreign key constraints :
ALTER TABLE DBRepoAuthor ADD CONSTRAINT DBRepoAuthor_FK1 FOREIGN KEY (repoID) REFERENCES REPOS (id);
ALTER TABLE DBRepoAuthor ADD CONSTRAINT DBRepoAuthor_FK2 FOREIGN KEY (authorID) REFERENCES AUTHORS (id);

-- composite key indexes :
ALTER TABLE DBRepoAuthor ADD CONSTRAINT DBRepoAuthor_CPK UNIQUE (repoID, authorID);

-- column group indexes :
CREATE UNIQUE INDEX DBRepoAuthorIDX1 ON DBRepoAuthor (repoID, repoAuthorName);


# --- !Downs

DROP TABLE DBRepoAuthor;
