# --- !Ups

CREATE TABLE REVISIONS (
  id             BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
  author         VARCHAR(128),
  logMessage     VARCHAR(2048) NOT NULL,
  revisionNumber BIGINT        NOT NULL,
  date           TIMESTAMP     NOT NULL,
  repoID         BIGINT        NOT NULL
);

-- foreign key constraints :
ALTER TABLE REVISIONS ADD CONSTRAINT REVISIONS_FK1 FOREIGN KEY (repoID) REFERENCES REPOS (id);

-- indexes on REVISIONS
CREATE INDEX REVISIONS_IDX1 ON REVISIONS (revisionNumber);

-- column group indexes :
CREATE UNIQUE INDEX REVISIONS_CGX1 ON REVISIONS (repoID, revisionNumber);


# --- !Downs

DROP TABLE REVISIONS;
