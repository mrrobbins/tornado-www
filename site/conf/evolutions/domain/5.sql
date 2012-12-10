
# --- !Ups

INSERT INTO tornado_wind_speed (
	rating, minimum_speed
) VALUES (
	0, 65
), (
	1, 86
), (
	2, 111
), (
	3, 136
), (
	4, 166
), (
	5, 200
);

# --- !Downs

DELETE FROM tornado_wind_speed;

