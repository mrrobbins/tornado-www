# --- !Ups

CREATE TABLE report (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	picture_path VARCHAR(1024),
	time INTEGER NOT NULL, -- UNIX time
	latitude DOUBLE NOT NULL,
	longitude DOUBLE NOT NULL,
	address VARCHAR(1024) NOT NULL,
	location_description VARCHAR(1024) NOT NULL,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL
);

CREATE TABLE tornado_damage_indicator (
	indicator_number INTEGER PRIMARY KEY,
	description VARCHAR(1024),
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

DROP TABLE report;
DROP TABLE tornado_degree_of_damage;
DROP TABLE tornado_damage_indicator;

