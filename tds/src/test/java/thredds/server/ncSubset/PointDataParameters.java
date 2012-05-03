package thredds.server.ncSubset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

public final class PointDataParameters {

	private static  List<List<String>> vars;
	private static List<String> pathInfo;
	private static List<LatLonPoint> point;
	
	//Index for the vertical level. verticalLevel < 0 means all levels.
	//We must now beforehand the size of the vertical level use index within the Axis range
	private static List<Double> verticalLevels;

	private PointDataParameters(){}
		
	static{
		

			vars = new ArrayList<List<String>>();			
			//Variables with no vertical level
			vars.add(Arrays.asList(new String[]{"Convective_Available_Potential_Energy_surface", "Pressure_surface"}));			
			//Variables with one vertical level
			vars.add(Arrays.asList(new String[]{"Relative_humidity_height_above_ground", "Temperature_height_above_ground"}));			
			//Variables with multiple vertical levels (hPa)
			vars.add(Arrays.asList(new String[]{"Temperature_isobaric", "Relative_humidity_isobaric"}));			
			
			pathInfo = new ArrayList<String>();									
			//dataset must contain the corresponding variables array
			pathInfo =Arrays.asList( new String[]{"/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1", 
												  "/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1",
												  "/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1"
					});
			
			//Points must be within the dataset boundaries
			point = new ArrayList<LatLonPoint>();
			point.add(new LatLonPointImpl( 42.0, -105.2 ));
			point.add(new LatLonPointImpl( 50.0,-100.2 ));			
			point.add(new LatLonPointImpl( 18.0,-102.25 ));
		
			verticalLevels=Arrays.asList( new Double[]{-1.0, 10.0, -1.0}  );
	};
	
	
	public static List<List<String>>  getVars(){
		return vars;
	}
	
	public static List<String>  getPathInfo(){
		return pathInfo;
	}	
	
	
	public static List<LatLonPoint> getPoints(){
		return point;
	}
	
	public static List<Double> getVerticalLevels(){
		return verticalLevels;
	}
		
}
