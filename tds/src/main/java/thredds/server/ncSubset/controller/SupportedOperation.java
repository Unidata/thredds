package thredds.server.ncSubset.controller;

/**
 * netcdf subset service allows 3 kinds of operations
 *  
 * @author mhermida
 *
 */
enum SupportedOperation {
	
	DATASET_INFO_REQUEST("Dataset info request"),
	POINT_REQUEST("Point data request"),
	GRID_REQUEST("Grid data request");
	
	private final String operationName; 
	
	SupportedOperation(String operationName){
		
		this.operationName = operationName;
	}

	String getOperation(){
		return operationName;
	}
}
