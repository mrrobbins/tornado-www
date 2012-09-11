var map;

function initialize(elementId) {
	var mapOptions = {
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	map = new google.maps.Map(document.getElementById(elementId),
			mapOptions);
	geocodeAddress("United States", centerAndZoomCallback(5));
}

function geocodeAddressForm(addressId, resultAreaId) {
	function processResult(result, state) {
		centerAndZoomCallback(12)(result, state);

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
			var onClickAction = "centerAndZoomMap(" + result[i].geometry.location.lat() + ", " + result[i].geometry.location.lng() + ", 12)";
			anchor.setAttribute("onclick", onClickAction);

			item.appendChild(anchor);

			resultList.appendChild(item);
		}

		resultArea.appendChild(resultList);
	}

	var address = document.getElementById(addressId).value;
	if (address.length != 0) {
		geocodeAddress(address, processResult);
	} else {
		alert("Please enter a location");
	}

	return false;
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
			alert("Location not found: " + anAddress)
		}
	}
}

function centerAndZoomMap(lat, lng, zoom)
{
	map.setCenter(new google.maps.LatLng(lat, lng));
	map.setZoom(zoom);
}
