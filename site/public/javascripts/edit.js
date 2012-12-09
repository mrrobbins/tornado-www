
$(function() {

	function populateDegree(indicator) {
		$.getJSON("/api/indicators/"+indicator+"/degrees", function(d) {
			var degree = $('#degree');
			degree.empty();
			var none = $(document.createElement('option'));
			none.attr('value', '0');
			none.append('None');
			degree.append(none);
			for (var i in d) {
				var id = d[i].id;
				var description = d[i].description;
				var option = $(document.createElement('option'))
				option.attr('value', id);
				option.append(id+" - "+description)
				degree.append(option)
			}
		} );
	};

	$('#indicator').change(function(eventObject) {
		populateDegree(this.value);
	} );

	$('#save_btn').click(function() {
		var form = $('#image_form');
		var imageId = form.attr('action');
		form.attr('action', '/images/edit/' + imageId);
		form.submit();
	});
	$('#delete_btn').click(function() {
		var form = $('#image_form');
		var imageId = form.attr('action');
		form.attr('action', '/api/images/delete/' + imageId);
		form.submit();
	});

} );$
