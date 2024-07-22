CREATE TABLE OPERATIONAL_MESSAGE_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   Id TEXT NOT NULL,
   Text TEXT NOT NULL,
   Source TEXT,
   Severity SMALLINT NOT NULL,
   LinkedEntityId INTEGER,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId, GenerationTime)
);
-- SEPARATOR
SELECT create_hypertable('public.OPERATIONAL_MESSAGE_TABLE', 'generationtime');
-- SEPARATOR
CREATE TABLE ACK_MESSAGE_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   MessageId BIGINT NOT NULL,
   State SMALLINT NOT NULL,
   UserName TEXT,
   AcknowledgementTime TIMESTAMPTZ,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId)
);
-- SEPARATOR
CREATE TABLE EVENT_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   ExternalId INTEGER NOT NULL,
   Name TEXT NOT NULL,
   Path TEXT NOT NULL,
   Qualifier TEXT,
   ReceptionTime TIMESTAMPTZ NOT NULL,
   Type TEXT NOT NULL,
   Route TEXT,
   Source TEXT,
   Severity SMALLINT NOT NULL,
   ContainerId BIGINT,
   Report BYTEA,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId, GenerationTime)
);
-- SEPARATOR
SELECT create_hypertable('public.EVENT_DATA_TABLE',  'generationtime');
-- SEPARATOR
CREATE TABLE RAW_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   Name TEXT NOT NULL,
   ReceptionTime TIMESTAMPTZ NOT NULL,
   Type TEXT NOT NULL,
   Route TEXT,
   Source TEXT,
   Handler TEXT NOT NULL,
   Quality SMALLINT NOT NULL,
   RelatedItem BIGINT,
   Contents BYTEA,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId, GenerationTime)
);
-- SEPARATOR
SELECT create_hypertable('public.RAW_DATA_TABLE', 'generationtime');
-- SEPARATOR
CREATE TABLE PARAMETER_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   ExternalId INTEGER NOT NULL,
   Name TEXT NOT NULL,
   Path TEXT NOT NULL,
   EngValue BYTEA,
   SourceValue BYTEA,
   ReceptionTime TIMESTAMPTZ NOT NULL,
   Route TEXT,
   Validity SMALLINT NOT NULL,
   AlarmState SMALLINT NOT NULL,
   ContainerId BIGINT,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId, GenerationTime)
);
-- SEPARATOR
SELECT create_hypertable('public.PARAMETER_DATA_TABLE', 'generationtime');
-- SEPARATOR
CREATE TABLE ALARM_PARAMETER_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   ExternalId INTEGER NOT NULL,
   Name TEXT NOT NULL,
   Path TEXT NOT NULL,
   CurrentAlarmState SMALLINT NOT NULL,
   CurrentValue BYTEA,
   ReceptionTime TIMESTAMPTZ NOT NULL,
   LastNominalValue BYTEA,
   LastNominalValueTime TIMESTAMPTZ,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId, GenerationTime)
);
-- SEPARATOR
SELECT create_hypertable('public.ALARM_PARAMETER_DATA_TABLE', 'generationtime');
-- SEPARATOR
CREATE TABLE ACTIVITY_OCCURRENCE_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   ExternalId INTEGER NOT NULL,
   Name TEXT NOT NULL,
   Path TEXT NOT NULL,
   Type TEXT NOT NULL,
   Route TEXT NOT NULL,
   Source TEXT,
   Arguments BYTEA,
   Properties BYTEA,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId)
);
-- SEPARATOR
CREATE TABLE ACTIVITY_REPORT_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   Name TEXT NOT NULL,
   ExecutionTime TIMESTAMPTZ,
   State SMALLINT NOT NULL,
   NextState SMALLINT NOT NULL,
   ReportStatus SMALLINT NOT NULL,
   Result BYTEA,
   ActivityOccurrenceId BIGINT REFERENCES ACTIVITY_OCCURRENCE_DATA_TABLE(UniqueId),
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId)
);
-- SEPARATOR
CREATE INDEX PARAMETER_DATA_TABLE_IDX1 ON PARAMETER_DATA_TABLE (GenerationTime ASC);
-- SEPARATOR
CREATE TABLE SCHEDULED_ACTIVITY_DATA_TABLE (
   UniqueId BIGINT NOT NULL,
   GenerationTime TIMESTAMPTZ NOT NULL,
   ActivityRequest BYTEA NOT NULL,
   Path TEXT NOT NULL,
   ActivityOccurrence BIGINT,
   Resources TEXT NOT NULL,
   Source TEXT NOT NULL,
   ExternalId TEXT NOT NULL,
   Trigger BYTEA NOT NULL,
   LatestInvocationTime TIMESTAMPTZ,
   StartTime TIMESTAMPTZ NOT NULL,
   Duration INTEGER NOT NULL,
   ConflictStrategy SMALLINT NOT NULL,
   State SMALLINT NOT NULL,
   AdditionalData BYTEA,
   PRIMARY KEY (UniqueId)
);