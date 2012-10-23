# --- !Ups

-- Relates case studies to collections m:n
CREATE TABLE collection_study ( 
	case_study_id BIGINT NOT NULL,
	collection_id BIGINT NOT NULL,
	PRIMARY KEY(collection_id, case_study_id),
	FOREIGN KEY(case_study_id) REFERENCES case_study(id),
	FOREIGN KEY(collection_id) REFERENCES collection(id)
);

-- Relates images to collections m:n
CREATE TABLE collection_image (
	collection_id BIGINT NOT NULL,
	image_id BIGINT NOT NULL,
	notes VARCHAR(10240) NOT NULL,
	damage_indicator INTEGER NOT NULL,
	degree_of_damage INTEGER NOT NULL,
	PRIMARY KEY(collection_id, image_id),
	FOREIGN KEY (collection_id) REFERENCES collection (id),
	FOREIGN KEY (image_id) REFERENCES image (id)
);

# --- !Downs
DROP TABLE collection_study;
DROP TABLE collection_image;
