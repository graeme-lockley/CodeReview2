# --- !Ups

ALTER TABLE AUTHORS ADD EmailAddress VARCHAR(128);
ALTER TABLE AUTHORS ADD IsAdmin BOOLEAN DEFAULT FALSE;


# --- !Downs

ALTER TABLE AUTHORS DROP COLUMN EmailAddress;
ALTER TABLE AUTHORS DROP COLUMN IsAdmin;
