PRAGMA ENCODING = 'UTF-8';

-- <editor-fold desc="Location">
DROP TABLE IF EXISTS Location;
DROP INDEX IF EXISTS Location_id_index;

CREATE TABLE IF NOT EXISTS Location
(
    id          INTEGER  DEFAULT 0     NOT NULL
        CONSTRAINT Location_pk PRIMARY KEY AUTOINCREMENT,
    name        TEXT     DEFAULT ''    NOT NULL,
    level       INTEGER  DEFAULT 0     NOT NULL,
    parent_id   INTEGER  DEFAULT 0     NOT NULL,
    create_time DATETIME DEFAULT (DATETIME()),
    update_time DATETIME DEFAULT (DATETIME()),
    deleted     BOOLEAN  DEFAULT FALSE NOT NULL
);
CREATE INDEX Location_id_index ON Location (id);

INSERT INTO Location (name, level, parent_id, create_time, update_time, deleted)
VALUES ('家', 0, 0, DATETIME(), DATETIME(), FALSE);

INSERT INTO Location (name, level, parent_id, create_time, update_time, deleted)
VALUES ('客厅', 1, 0, DATETIME(), DATETIME(), FALSE);
INSERT INTO Location (name, level, parent_id, create_time, update_time, deleted)
VALUES ('卧室', 1, 0, DATETIME(), DATETIME(), FALSE);
-- </editor-fold>

-- <editor-fold desc="Scheduled Task">
DROP TABLE IF EXISTS ScheduledTask;
DROP INDEX IF EXISTS ScheduledTask_id_index;

CREATE TABLE IF NOT EXISTS ScheduledTask
(
    id          INTEGER  DEFAULT 0           NOT NULL
        CONSTRAINT ScheduledTask_pk PRIMARY KEY AUTOINCREMENT,
    name        TEXT     DEFAULT ''          NOT NULL,
    create_time DATETIME DEFAULT (DATETIME()),
    cron        TEXT     DEFAULT '0 0 0 0 0' NOT NULL
);

CREATE INDEX ScheduledTask_id_index ON ScheduledTask (id);

INSERT INTO ScheduledTask (name, create_time, cron)
VALUES ('打扫房间', DATETIME(), '0 21 * * Wed');
-- </editor-fold>

-- <editor-fold desc="Scheduled Task Exec">
DROP TABLE IF EXISTS ScheduledTaskExec;
DROP INDEX IF EXISTS ScheduledTaskHistory_id_index;

CREATE TABLE IF NOT EXISTS ScheduledTaskHistory
(
    id        INTEGER  DEFAULT 0          NOT NULL
        CONSTRAINT ScheduledTaskHistory_pk PRIMARY KEY AUTOINCREMENT,
    task_id   INTEGER  DEFAULT 0          NOT NULL
        CONSTRAINT ScheduledTaskHistory_ScheduledTask_id_fk
            REFERENCES ScheduledTask (id),
    done_time DATETIME DEFAULT (DATETIME()) NOT NULL
);

CREATE INDEX ScheduledTaskHistory_id_index ON ScheduledTaskHistory (id);
-- </editor-fold>
