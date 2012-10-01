# --- !Ups

CREATE TABLE tornado_damage_indicator (
	indicator_number INTEGER PRIMARY KEY,
	description VARCHAR(1024) NOT NULL,
	abbreviation VARCHAR(16) UNIQUE
);

CREATE TABLE tornado_degree_of_damage (
	degree_number INTEGER,
	indicator_abbreviation VARCHAR(16),
	description VARCHAR(1024),
	lowest_windspeed INTEGER,
	expected_windspeed INTEGER,
	highest_windspeed INTEGER,
	PRIMARY KEY(degree_number, indicator_abbreviation),
	FOREIGN KEY(
		indicator_abbreviation
	) REFERENCES tornado_damage_indicator(
		abbreviation
	)
);


-- Begin Insertions (Data from NOAA)

-- Insert Damage Indicators


# --- !Downs

DROP TABLE tornado_degree_of_damage;
DROP TABLE tornado_damage_indicator;

