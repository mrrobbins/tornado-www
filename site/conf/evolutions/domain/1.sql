
# --- !Ups

CREATE TABLE tornado_damage_indicator (
  _id INTEGER PRIMARY KEY,
  description TEXT,
  abbreviation VARCHAR(64) UNIQUE
);

# --- !Downs

DROP TABLE tornado_damage_indicator;

