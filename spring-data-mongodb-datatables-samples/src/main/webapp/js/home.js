$(document).ready(function() {
	var table = $('table#sample').DataTable({
		'ajax' : {
			'url': '/data/orders',
			'data': function(data){
				if($('#cb_contain_date').prop('checked')){
					data.startDate = $('#start_date').val();
					data.endDate = $('#end_date').val();
				}
				return data;
			}
		},
		'serverSide' : true,
		'columns' : [ {
			'data' : 'id'
		}, {
			'data' : 'date'
		}, {
			'data' : 'orderNumber'
		}, {
			'data' : 'isValid'
		}, {
			'data' : 'amount'
		}, {
			'data' : 'price'
		}]
	});

	$('select#orderNumber_selector').change(function() {
		table.columns(2).search(getSelectFilter('orderNumber_selector')).draw();
	});

	$('select#isValid_selector').change(function() {
		table.columns(3).search(getSelectFilter('isValid_selector')).draw();
	});

	$('#btn_submit').click(function(){
		draw();
	});
	
	function getSelectFilter(selectorId){
		var filterArray = [];
		$('#' + selectorId).children('option:selected').each(function(){
			filterArray.push( $(this).val() );
		});
		return filterArray.join("+");
	}
	
	function draw(){
		table.columns(2).search(getSelectFilter('orderNumber_selector'));
		table.columns(3).search(getSelectFilter('isValid_selector'));
		table.draw();
	}

});