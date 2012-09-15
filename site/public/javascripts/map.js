var map;
var selectResult; //holds result set state, takes new item selection index

function initialize(elementId) {
	var mapOptions = {
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	map = new google.maps.Map(document.getElementById(elementId), mapOptions);
	geocodeAddress("United States", centerAndZoomCallback(5));
}

function geocodeAddressForm(addressId, resultAreaId) {
	function processResult(result, state) {
		centerAndZoomCallback(12)(result, state);
		buildResultList(result, resultAreaId, 0);
	}

	var address = document.getElementById(addressId).value;
	if (address.length != 0) {
		geocodeAddress(address, processResult);
	} else {
		alert("Please enter a location");
	}

	return false;
}

function buildResultList(result, resultAreaId, selectionIndex) {
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

	selectResult = function (selectionIndex) { 
		buildResultList(result, resultAreaId, selectionIndex); 
		map.setCenter(result[selectionIndex].geometry.location);
		map.setZoom(12);
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
		console.log("State:" + state);
		console.log(results);

		if (state == google.maps.GeocoderStatus.OK) {
			var latLng = results[0].geometry.location;
			map.setCenter(latLng)
			map.setZoom(zoom)
		} else {
			alert("Search returned no results!")
		}
	}
}

function selectResult(selection) {
	centerAndZoomMap(lat, lng, 12);
	buildListResult(selection);
}

