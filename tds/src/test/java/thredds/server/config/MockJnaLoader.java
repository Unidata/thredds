package thredds.server.config;

import thredds.server.config.Netcdf4AvailabilityChecker;
import ucar.nc2.jni.netcdf.Nc4Iosp;

public class MockJnaLoader {
	
	public static void loadJnaLibrary(){

		String jnaPath = System.getProperty("jna.library.path");
		Nc4Iosp.setLibraryAndPath(jnaPath, "netcdf");
		Netcdf4AvailabilityChecker.setNetcdf4Available(Nc4Iosp.isClibraryPresent() );		
	}

}
