package thredds.mock.params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

public final class PointDataParameters {

	private static  List<List<String>> vars;
	private static List<Map<String, List<String>> >groupedVars;
	private static List<String> pathInfo;
	private static List<LatLonPoint> point;
	
	//Index for the vertical level. verticalLevel < 0 means all levels.
	//We must now beforehand the size of the vertical level use index within the Axis range
	private static List<Double> verticalLevels;

	private PointDataParameters(){}
		
	static{
		

			vars = new ArrayList<List<String>>();			
			//Variables with no vertical level
			vars.add(Arrays.asList(new String[]{"Pressure", "Pressure_reduced_to_MSL"}));
			//vars.add(Arrays.asList(new String[]{"Pressure"}));
			//Variables with one vertical level
			vars.add(Arrays.asList(new String[]{"Relative_humidity_height_above_ground", "Temperature_height_above_ground"}));			
			//Variables with multiple vertical levels (hPa)
			vars.add(Arrays.asList(new String[]{"Temperature", "Relative_humidity"}));			
			
			//Variables with different vertical levels
			vars.add(Arrays.asList(new String[]{"Pressure", "Relative_humidity_height_above_ground", "Temperature"}));
			
			//Variables for NARR dataset
			vars.add(Arrays.asList(new String[]{"TMP_200mb"}));
			
			pathInfo = new ArrayList<String>();									
			//dataset must contain the corresponding variables array
			pathInfo =Arrays.asList( new String[]{"/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z", 
												  "/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z",
												  //"testFeatureCollection/files/GFS_CONUS_80km_20120418_1200.nc",
												  "/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z",
												  "/testNCSS/narr-TMP-200mb_221_yyyymmdd_hh00_000.grb.grb2.nc4"
					});
			
			Map<String,List<String>> noVertLevels = new HashMap<String,List<String> >();
			noVertLevels.put("no_vert_levels", vars.get(0) );
			
			Map<String,List<String>> heightAvobeGround = new HashMap<String,List<String> >();
			heightAvobeGround.put("height_above_ground", vars.get(1) );
			
			Map<String,List<String>> isobaric = new HashMap<String,List<String> >();
			isobaric.put("isobaric", vars.get(2) );
			
			Map<String,List<String>> narrVars = new HashMap<String,List<String> >();
			narrVars.put("narrVars", vars.get(4) );			
			
			groupedVars = new ArrayList<Map<String, List<String>> >();
			groupedVars.add(noVertLevels);
			groupedVars.add(heightAvobeGround);
			groupedVars.add(isobaric);
			groupedVars.add(narrVars);
			
			//Points must be within the dataset boundaries
			point = new ArrayList<LatLonPoint>();
			point.add(new LatLonPointImpl( 42.0, -105.2 ));			
			point.add(new LatLonPointImpl( 50.0,-100.2 ));			
			point.add(new LatLonPointImpl( 18.0,-102.25 ));
			point.add(new LatLonPointImpl( 40.019,-105.293 ));
		
			verticalLevels=Arrays.asList( new Double[]{-1.0, 10.0, -1.0}  );
	};
	
	
	public static List<List<String>>  getVars(){
		return vars;
	}
	
	public static List<Map<String, List<String>>>  getGroupedVars(){
		return groupedVars;
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
