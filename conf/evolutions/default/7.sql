# --- !Ups

CREATE TABLE REVISION_ENTRIES_CONTENT (
  id      BIGINT NOT NULL PRIMARY KEY,
  content CLOB   NOT NULL
);

-- foreign key constraints :
ALTER TABLE REVISION_ENTRIES_CONTENT ADD CONSTRAINT REVISION_ENTRIES_CONTENT_FK1 FOREIGN KEY (id) REFERENCES REVISION_ENTRIES (id);


# --- !Downs

DROP TABLE REVISION_ENTRIES_CONTENT;