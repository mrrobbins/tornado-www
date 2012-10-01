# --- !Ups

ALTER TABLE pending_image ADD CONSTRAINT
valid_image CHECK (image_id NOT IN (SELECT primary_image_id FROM collection));

# --- !Downs


ALTER TABLE collection DROP CONSTRAINT valid_image;

