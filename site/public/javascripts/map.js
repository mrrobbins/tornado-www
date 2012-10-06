
var mapOptions = {
	mapTypeId: google.maps.MapTypeId.ROADMAP
};

var geocoder = new google.maps.Geocoder()

var gcStatus = google.maps.GeocoderStatus

var markerDescriptors = [];
var markers = {};


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

	queryMarkers();

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
				else searchFor(searchText, map);

			});

			$('#search_box').click(function() {
				$("#sidebar").show(100, function() {
					resizeMap();
				});
			});

			searchFor(searchText, map);
		}
	});

	function queryMarkers() {
		$.getJSON("/api/map/markers/all", function(data) {
			markerDescriptors = data.markers;
			redrawMarkers();
		});
	}

	function forEachMarker(func) {
		for (var key in markers) {
			if (markers.hasOwnProperty(key)) {
				var group = markers[key];
				for (var i in group) {
					func(group[i])
				}
			}
		}
	}

	function showMarkerGroup(group) {
		if (markers[group] == undefined) return;
		for (var i in markers[group]) {
			markers[group][i].setMap(map);
		}
	}

	function hideMarkerGroup(group) {
		if (markers[group] == undefined) return;
		for (var i in markers[group]) {
			markers[group][i].setMap(null);
		}
	}

	function clearMarkerGroup(group) {
		hideMarkerGroup(group);
		delete markers[group];
	}

	function addToMarkerGroup(group, marker) {
		if (markers[group] == undefined) markers[group] = [];
		markers[group].push(marker);
	}

	function clearAllMarkers() {
		forEachMarker(function (marker) {
			marker.setMap(null);
		});
		markers = { };
	}

	function redrawMarkers() {

			clearAllMarkers();
			for (var i in markerDescriptors) {
				var desc = markerDescriptors[i];
				var pos = new google.maps.LatLng(desc.lat, desc.long);
				var marker = new google.maps.Marker({
					position: pos,
					title: desc.name
				});
				marker.setMap(map);
				if (markers[desc.type] == undefined) markers[desc.type] = [];
				markers[desc.type].push(marker)
			}
	}

	function searchFor(searchString) {
		geocodeAddress(searchString, function(results, status) {
			console.log('here');
			if (gcStatus.OK == status && results.length != 0) {
				populateResultList(results, 0, map);
				jumpToResult(results[0], map);
				setResultMarker(results[0]);
			} else {
				fillResultArea("No Results")
			}
		});
	};

	function populateResultList(geocodingResults, selectedIndex) {

		function makeHandler(index) {
			return function (eventObj) {
				eventObj.preventDefault();
				console.log('clicked ' + index);
				populateResultList(geocodingResults, index, map);
				jumpToResult(geocodingResults[index], map);
				setResultMarker(geocodingResults[index]);
			}
		}

		var list = $(document.createElement('ol'));
		for (var i in geocodingResults) {
			var li = $(document.createElement('li'));
			var result = createResult(geocodingResults[i], i)
			result.click(makeHandler(i));
			if (i == selectedIndex) {
				result.addClass('selected-result');
			}
			li.append(result[0]);
			list.append(li[0]);
		}
		fillResultArea(list[0]);
	}

	function jumpToResult(geocodingResult) {
		var geom = geocodingResult.geometry
		map.setCenter(geom.location);
		map.fitBounds(geom.viewport);
	}

	function setResultMarker(geocodingResult) {
		var geom = geocodingResult.geometry
		var marker = new google.maps.Marker({
			title: geocodingResult.formatted_address,
			position: geom.location
		});
		marker.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png')
		marker.setAnimation(google.maps.Animation.DROP)
		clearMarkerGroup('results');
		addToMarkerGroup('results', marker);
		showMarkerGroup('results');
	}

});

function geocodeAddress(address, callback) {
	var request = {
		address: address
	};

	geocoder.geocode(request, callback);
}

function fillResultArea(contents) {
	var panel = $('#geocode_result');
	panel.empty();
	panel.append(contents);
}


function createResult(geocodingResult) {
	var result = $(document.createElement("a"));
	result.append(geocodingResult.formatted_address);
	return result;
}


