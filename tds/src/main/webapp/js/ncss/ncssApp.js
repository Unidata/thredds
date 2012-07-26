

Ncss.log("Ncss loading...");

Ncss.changeTemporalSubsetting = function(){

	var timeRangeTab = $('#inputTimeRange');
	var singleTimeTab = $('#inputSingleTime');	

	var temporalSubset =$('#timeRangeSubset');
	var singleTimeSubset =$('#singleTimeSubset'); 
	
	if( timeRangeTab.attr('class') == "selected" ){
		timeRangeTab.removeClass("selected").addClass("unselected");
				
		singleTimeTab.removeClass("unselected").addClass("selected");
		
		temporalSubset.addClass('hidden');
		singleTimeSubset.removeClass('hidden');
		
		$('input[name=time]').removeAttr("disabled");
		
		$('input[name=time_start]').attr("disabled","disabled");
		$('input[name=time_end]').attr("disabled","disabled");
		$('input[name=timeStride]').attr("disabled","disabled");
		
	}else{
		timeRangeTab.removeClass("unselected").addClass("selected");
				
		singleTimeTab.removeClass("selected").addClass("unselected");
		
		singleTimeSubset.addClass('hidden');
		temporalSubset.removeClass('hidden');
		
		$('input[name=time]').attr("disabled","disabled");
		
		$('input[name=time_start]').removeAttr("disabled");
		$('input[name=time_end]').removeAttr("disabled");
		$('input[name=timeStride]').removeAttr("disabled");		
	}
	
};
