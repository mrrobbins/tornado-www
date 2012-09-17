# --- !Ups

CREATE TABLE report (
	id INTEGER PRIMARY KEY,
	picture_path VARCHAR(1024),
	time INTEGER, -- UNIX time
	latitude DOUBLE,
	longitude DOUBLE,
	address varchar(1024),
	location_description varchar(1024),
	notes varchar(10240),
	uploaded INTEGER, -- Boolean: whether the report has been uploaded
	damage_indicator INTEGER,
	degree_of_damage INTEGER
);

CREATE TABLE tornado_damage_indicator (
	indicator_number INTEGER PRIMARY KEY,
	description varchar(1024),
	abbreviation varchar(16) UNIQUE
);

CREATE TABLE tornado_degree_of_damage (
	degree_number INTEGER,
	indicator_abbreviation varchar(16),
	description varchar(1024),
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

