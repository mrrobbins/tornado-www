
# --- !Ups

CREATE TABLE tornado_damage_indicator (
  id INTEGER PRIMARY KEY,
  description TEXT NOT NULL,
  abbreviation VARCHAR(64) UNIQUE NOT NULL
);

# --- !Downs

DROP TABLE tornado_damage_indicator;

