# --- !Ups

CREATE TABLE pending_image (
	image_id INTEGER PRIMARY KEY,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	FOREIGN KEY (image_id) REFERENCES image (id)
);

# --- !Downs

DROP TABLE pending_image;

