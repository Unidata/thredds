package thredds.server.ncSubset.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

final class PointDataWritersParameters {

	private static  List<List<String>> vars;
	//private List<GridDataset> datasets;
	//private List<GridAsPointDataset> gridsAsPointDataset;
	//private List<List<CalendarDate>> dateRanges;
	//private List<CalendarDate> dates;
	private static List<String> pathInfo;
	private static List<LatLonPoint> point;
	//private List<CoordinateAxis1D> verticalAxis;
	

	private PointDataWritersParameters(){}
		
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
			point.add(new LatLonPointImpl( 12.0,-49.2 ));
		
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
		
}
