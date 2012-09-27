# --- !Ups

CREATE TABLE collection (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	primary_image_id INTEGER,
	time_created INTEGER NOT NULL, -- UNIX time
	gps_address VARCHAR(1024) NOT NULL,
	address VARCHAR(1024) NOT NULL,
	location_description VARCHAR(1024) NOT NULL,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	FOREIGN KEY(primary_image_id) REFERENCES image(id)
);

CREATE TABLE case_study (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(256),
	description VARCHAR(10240) 
);

-- Relates case studies to collections m:n
CREATE TABLE collection_study ( 
	case_study_id INTEGER,
	collection_id INTEGER,
	PRIMARY KEY(collection_id, case_study_id),
	FOREIGN KEY(case_study_id) REFERENCES case_study(id),
	FOREIGN KEY(collection_id) REFERENCES collection(id)
);

-- Relates images to collections m:n
CREATE TABLE collection_image (
	collection_id INTEGER,
	image_id INTEGER,
	notes VARCHAR(10240),
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	PRIMARY KEY(collection_id, image_id),
	FOREIGN KEY (collection_id) REFERENCES collection (id),
	FOREIGN KEY (image_id) REFERENCES image (id)
);

CREATE TABLE image (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	picture_path VARCHAR(1024),
	time_captured INTEGER NOT NULL, -- UNIX time
	latitude DOUBLE NOT NULL,
	longitude DOUBLE NOT NULL,
	user_id INTEGER,
	FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE user (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	username VARCHAR(32),
	admin BOOLEAN,
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

DROP TABLE collection;
DROP TABLE tornado_degree_of_damage;
DROP TABLE tornado_damage_indicator;
DROP TABLE case_study;
DROP TABLE collection_study;
DROP TABLE collection_image;
DROP TABLE image;
DROP TABLE user;

