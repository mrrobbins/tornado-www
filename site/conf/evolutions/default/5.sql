# --- !Ups

CREATE TABLE collection (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	time_created INTEGER NOT NULL, -- Milliseconds since the epoch
	name VARCHAR(50) UNIQUE NOT NULL,
	creator BIGINT NOT NULL,
	FOREIGN KEY (creator) REFERENCES user (id)
);

# --- !Downs


DROP TABLE collection;

