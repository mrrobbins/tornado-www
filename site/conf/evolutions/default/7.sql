# --- !Ups

ALTER TABLE pending_image ADD CONSTRAINT
not_in_collection CHECK (image_id NOT IN (SELECT image_id FROM collection_image));

ALTER TABLE collection_image ADD CONSTRAINT
not_pending CHECK (image_id NOT IN (SELECT image_id FROM pending_image));

# --- !Downs


ALTER TABLE pending_image DROP CONSTRAINT not_in_collection;
ALTER TABLE collection_image DROP CONSTRAINT not_pending;

