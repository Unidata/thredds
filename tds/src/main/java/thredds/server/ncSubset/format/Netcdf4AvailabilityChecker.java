package thredds.server.ncSubset.format;

import ucar.nc2.jni.netcdf.Nc4Iosp;

class Netcdf4AvailabilityChecker {
	
	private Netcdf4AvailabilityChecker(){
		
	}
	
	static boolean isNetcdf4Available(){
		boolean isAvailable = false;
		
		try{
			
			isAvailable = Nc4Iosp.isClibraryPresent();
			
		}catch(UnsatisfiedLinkError err){
			//Catches the error and swallows it...
			//If the netcdf4 library is not available we just mark the format as unavailable  
		}
		
		return isAvailable;		
	}

}
