package thredds.server.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import thredds.server.ncSubset.format.SupportedFormat;

@Service
public final class FormatsAvailabilityService {
	
	private static Map<SupportedFormat, Boolean> formatsAvailability = new HashMap<SupportedFormat, Boolean>();
	
	static{
		//Default availabiliy
		formatsAvailability.put(SupportedFormat.XML , true);
		formatsAvailability.put(SupportedFormat.CSV , true);
		formatsAvailability.put(SupportedFormat.NETCDF3 , true);
		formatsAvailability.put(SupportedFormat.NETCDF4 , false);
		formatsAvailability.put(SupportedFormat.WKT , true);
		formatsAvailability.put(SupportedFormat.JSON , true);
	}
	
	private FormatsAvailabilityService(){
		
	}
	
	public static boolean isFormatAvailable(SupportedFormat format){		
		return formatsAvailability.get(format); 
	}

	static void setFormatAvailability(SupportedFormat format, boolean available){
		
		formatsAvailability.put(format, available); 

	}	
}
