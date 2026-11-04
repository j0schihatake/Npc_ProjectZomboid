PRAGMA user_version = 1;
CREATE TABLE dialogue (
    id INTEGER PRIMARY KEY,
    topic TEXT NOT NULL,
    condition TEXT NOT NULL CHECK(condition IN ('default','low_morale','low_trust','brave')),
    text_ru TEXT NOT NULL CHECK(length(trim(text_ru)) > 0)
);
CREATE INDEX dialogue_topic ON dialogue(topic, condition);
