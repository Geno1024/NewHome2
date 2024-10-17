PRAGMA ENCODING = 'UTF-8';

DROP TABLE Location;

CREATE TABLE IF NOT EXISTS Location
(
    id          INTEGER  DEFAULT 0     NOT NULL
        CONSTRAINT Location_pk PRIMARY KEY AUTOINCREMENT,
    name        TEXT     DEFAULT ''    NOT NULL,
    level       INTEGER  DEFAULT 0     NOT NULL,
    parent_id   INTEGER  DEFAULT 0     NOT NULL,
    create_time DATETIME DEFAULT 0,
    update_time DATETIME DEFAULT 0,
    deleted     BOOLEAN  DEFAULT FALSE NOT NULL
);

INSERT INTO Location (name, level, parent_id, create_time, update_time, deleted)
VALUES ('房间', 0, 0, DATETIME(), DATETIME(), FALSE);
