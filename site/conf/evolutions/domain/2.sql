
# --- !Ups

CREATE TABLE tornado_degree_of_damage (
  _id INTEGER,
  indicator_abbreviation VARCHAR(64),
  description TEXT,
  lowest_windspeed INTEGER,
  expected_windspeed INTEGER,
  highest_windspeed INTEGER,
  PRIMARY KEY(_id, indicator_abbreviation),
  FOREIGN KEY(
    indicator_abbreviation
  ) REFERENCES tornado_damage_indicator(
    abbreviation
  )
);

# --- !Downs

DROP TABLE tornado_degree_of_damage;
