
$(window).load(function() {
	$('.add-btn').click(function() {
		var selected = [];
		$('#selection_form input:checked').each(function (idx, value) {
			selected.push(value.name);
		});
		var collection = $('.collection-box').attr('value');
		var ids = JSON.stringify(selected);
		var form = $('form#add_form');
		form.find('input[name=imageIds]').attr('value', ids);
		form.find('input[name=collectionName]').attr('value', collection);

		if (window.sessionStorage) {
			sessionStorage.prevCollection = collection;
		}

		form.submit();
		return false;
	});

	$('.create-btn').click(function() {
		var collection = $('.collection-box').attr('value');
		var form = $('form#create_form');
		form.find('input[name=name]').attr('value', collection);

		if (window.sessionStorage) {
			sessionStorage.prevCollection = collection;
		}

		form.submit();
		return false;
	});

	$('.delete-btn').click(function() {
		var collection = $('.collection-box').attr('value');
		var form = $('form#delete_form');
		form.find('input[name=name]').attr('value', collection);

		if (window.sessionStorage) {
			sessionStorage.removeItem('prevCollection');
		}

		form.submit();
		return false;
	});

	if (window.sessionStorage && sessionStorage.prevCollection) {
		$('.collection-box').attr('value', sessionStorage.prevCollection);
	}

});

