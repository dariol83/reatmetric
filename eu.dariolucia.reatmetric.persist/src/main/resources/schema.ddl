CREATE TABLE OPERATIONAL_MESSAGE_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMP NOT NULL,
   MessageId VARCHAR(32) NOT NULL,
   MessageText VARCHAR(255) NOT NULL,
   MessageSource VARCHAR(32),
   MessageSeverity SMALLINT,
   AdditionalData BLOB,
   PRIMARY KEY (UniqueId)
)