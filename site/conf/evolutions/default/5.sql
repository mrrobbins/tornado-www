# --- !Ups

CREATE TABLE collection (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	primary_image_id BIGINT,
	time_created INTEGER NOT NULL, -- UNIX time
	gps_address VARCHAR(1024) NOT NULL,
	address VARCHAR(1024) NOT NULL,
	location_description VARCHAR(1024) NOT NULL,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	FOREIGN KEY(primary_image_id) REFERENCES image(id)
);

# --- !Downs


DROP TABLE collection;

