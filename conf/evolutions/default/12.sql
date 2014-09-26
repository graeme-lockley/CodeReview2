# --- !Ups

CREATE TABLE EVENTS (
  id       BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  authorID BIGINT       NOT NULL,
  when     TIMESTAMP    NOT NULL,
  name     VARCHAR(100) NOT NULL,
  content  CLOB         NOT NULL
);

# --- !Downs

DROP TABLE EVENTS;