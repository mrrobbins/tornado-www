
# --- !Ups

CREATE TABLE tornado_wind_speed (
	rating INTEGER PRIMARY KEY,
	minimum_speed INTEGER NOT NULL,
);

# --- !Downs

DROP TABLE tornado_wind_speed;

