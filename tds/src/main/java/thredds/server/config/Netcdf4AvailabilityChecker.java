package thredds.server.config;


public final class Netcdf4AvailabilityChecker {
	
	private static boolean netcdf4LibAvailable = false;
	
	private Netcdf4AvailabilityChecker(){

	}
	
	public static boolean isNetcdf4Available(){
		
		return netcdf4LibAvailable; 

	}

	static void setNetcdf4Available(boolean available){
		
		netcdf4LibAvailable = available; 

	}	
}
