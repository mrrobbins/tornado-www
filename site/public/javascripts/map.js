var map;
var selectResult; //holds result set state, takes new item selection index
var removeMarker = function () {};
var markerLists = {};

function initialize(elementId) {
	var mapOptions = {
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	map = new google.maps.Map(document.getElementById(elementId), mapOptions);
	geocodeAddress("United States", centerAndZoomCallback(5));
	$(document).ready(showMarkers);
	$("#hide_sidebar_button").click(function() {
		$("#sidebar").hide()
	});

	$("#search_form").click(function() {
		$("#sidebar").show()
	});
	
}

function showMarkers() {
	function displayMarkers(data) {
		markers = data["markers"];
		for (i in markers) {
			console.debug(i)
			var marker = markers[i];
			var latLng = new google.maps.LatLng(marker["lat"], marker["long"]);
			var m = makeMarker("dbDefault", latLng, marker["name"]);
		}
	}

	$.getJSON("/api/map/markers/all", displayMarkers);
}

function geocodeAddressForm(addressId, resultAreaId) {
	function processResult(result, state) {
		if (state == google.maps.GeocoderStatus.OK) {
			buildResultList(result, resultAreaId, 0);
		} else {
			alert("Search returned no results!")
		}
	}

	var address = document.getElementById(addressId).value;
	if (address.length != 0) {
		geocodeAddress(address, processResult);
	} else {
		alert("Please enter a location");
	}

	return false;
}

function makeMarker(group, latLng, title) {
	var marker = new google.maps.Marker({
		position: latLng,
		title: title
	});
	
	if (group == "searchResult") {
		marker.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png')
		marker.setAnimation(google.maps.Animation.DROP)
	}
	
	marker.setMap(map);

	if (markerLists[group] == undefined) {
		markerLists[group] = [marker];
	} else {
		markerLists[group].push(marker);
	}
}

function hideMarkerGroup(group) {
	for (i in markerLists[group]) {
		console.log(markerLists[group][i])
		markerLists[group][i].setMap(null);
	}
}

function showMarkerGroup(group) {
	for (i in markerLists[group]) {
		markerLists[group][i].setMap(map);
	}
}

function  buildResultList(result, resultAreaId, selectionIndex) {
	var resultArea = document.getElementById(resultAreaId);
	while (resultArea.hasChildNodes()) {
		resultArea.removeChild(resultArea.lastChild);
	}

	var resultList = document.createElement("ol");

	for (i in result) {
		var item = document.createElement("li");
		var line = result[i].formatted_address;
		var anchor = document.createElement("a");

		anchor.appendChild(document.createTextNode(line));
		anchor.setAttribute("href", "javascript:void(0)");
		var onClickAction = "selectResult(" + i + ")";

		anchor.setAttribute("onclick", onClickAction);

		if (i == selectionIndex) {
			var style = document.createElement("strong");
			style.appendChild(anchor);
			item.appendChild(style);
		} else {
			item.appendChild(anchor);
		}

		resultList.appendChild(item);
	}

	resultArea.appendChild(resultList);

	if (selectionIndex != undefined) {
		var latLng = result[selectionIndex].geometry.location;
		map.setCenter(latLng);
		map.setZoom(11);
		hideMarkerGroup("searchResult");
		makeMarker("searchResult", latLng, result[selectionIndex].formatted_address);
	}

	selectResult = function (selectionIndex) { 
		buildResultList(result, resultAreaId, selectionIndex); 
	};
}

function geocodeAddress(anAddress, callback) {
	var Geocoder = new google.maps.Geocoder()
	var request = {
		address: anAddress
	};

	Geocoder.geocode(request, callback)
}

function centerAndZoomCallback(zoom) {
	return function (results, state) {
		var latLng = results[0].geometry.location;
		map.setCenter(latLng);
		map.setZoom(zoom);
	}
}

