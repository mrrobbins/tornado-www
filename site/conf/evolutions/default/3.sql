# --- !Ups

CREATE TABLE image (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	picture_path VARCHAR(1024) NOT NULL,
	time_captured BIGINT, -- UNIX time
	latitude DOUBLE NOT NULL,
	longitude DOUBLE NOT NULL,
	user_id INTEGER NOT NULL,
	FOREIGN KEY (user_id) REFERENCES user (id)
);



# --- !Downs

DROP TABLE image;

