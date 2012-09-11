var map;

function initialize() {
	var mapOptions = {
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	map = new google.maps.Map(document.getElementById("map_canvas"),
			mapOptions);
	moveMapToAddress("United States", 5);
}

function moveMapToAddress(anAddress, zoom) {
	function geocodeResponse(results, state) {
		console.log("State:" + state);
		console.log(results);

		if (state == google.maps.GeocoderStatus.OK) {
			var latLng = results[0].geometry.location;
			map.setCenter(latLng)
			map.setZoom(zoom)
		} else {
			alert("Location not found: " + anAddress)
		}
	}

	var Geocoder = new google.maps.Geocoder()
	var request = {
		address: anAddress
	};

	Geocoder.geocode(request, geocodeResponse)
}


