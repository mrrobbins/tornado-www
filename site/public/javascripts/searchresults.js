
var mapOptions = {
	mapTypeId: google.maps.MapTypeId.ROADMAP
};

var geocoder = new google.maps.Geocoder()

var gcStatus = google.maps.GeocoderStatus

$(window).load(function() {
	var map = new google.maps.Map($('#map_canvas')[0], mapOptions);

	function resizeMap() {
		google.maps.event.trigger(map, "resize");
	};

	resizeMap();

	geocodeAddress('United States', function(results, state) {
		if (gcStatus.OK == state) {
			var geom = results[0].geometry
			map.setCenter(geom.location);
			map.fitBounds(geom.viewport);
		}
	});

	showMarkers();

	$("#hide_sidebar_button").click(function() {
		$("#sidebar").hide(100, function() {
			resizeMap();
		});
	});

	$("#search_form").submit(function(eventObj) {
		eventObj.preventDefault();
		var searchText = $('#search_box')[0].value

		if (searchText.length == 0) {

			alert('Please enter a location');

		} else {

			$("#sidebar").show(100, function() {
				resizeMap();
			});

			$('#search_form').unbind('submit')

			$('#search_form').submit(function(eventObj) {
				eventObj.preventDefault();

				var searchText = $('#search_box')[0].value

				if (searchText.length == 0) alert('Please enter a location');
				else searchFor(searchText);

			});

			$('#search_box').click(function() {
				$("#sidebar").show(100, function() {
					resizeMap();
				});
			});

		}
	});
	}

);

function searchFor(searchString) {
	geocodeAddress(searchString, function(results, status) {
		console.log('here');
		var panel = $('geocode_result');
		panel.empty();
		if (gcStatus.OK == status) {
			panel.append('Good');
		} else {
			panel.append('Error');
		}
	});
};

function showMarkers() {
}

function geocodeAddress(address, callback) {
	var request = {
		address: address
	};

	geocoder.geocode(request, callback)
}

