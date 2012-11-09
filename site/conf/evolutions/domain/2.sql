
# --- !Ups

CREATE TABLE tornado_degree_of_damage (
  id INTEGER NOT NULL,
  indicator_abbreviation VARCHAR(64) NOT NULL,
  description TEXT NOT NULL,
  lowest_windspeed INTEGER NOT NULL,
  expected_windspeed INTEGER NOT NULL,
  highest_windspeed INTEGER NOT NULL,
  PRIMARY KEY(_id, indicator_abbreviation),
  FOREIGN KEY(
    indicator_abbreviation
  ) REFERENCES tornado_damage_indicator(
    abbreviation
  )
);

# --- !Downs

DROP TABLE tornado_degree_of_damage;
