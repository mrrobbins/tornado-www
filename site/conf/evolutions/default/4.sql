# --- !Ups

CREATE TABLE pending_image (
	image_id BIGINT PRIMARY KEY,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	user_id BIGINT NOT NULL,
	FOREIGN KEY (image_id) REFERENCES image (id),
	FOREIGN KEY (user_id) REFERENCES user (id)
);

# --- !Downs

DROP TABLE pending_image;

