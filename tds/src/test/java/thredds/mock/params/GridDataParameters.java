/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.mock.params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author marcos
 *
 */
public class GridDataParameters {
	
	private static  List<List<String>> vars;
	
	private static List<double[]> latlonRect; //Arrays containing west, south, east, north
	
	private static List<double[]> projectionRect; //Arrays containing minx, miny, maxx, maxy
	
	private GridDataParameters(){}
	
	static{
		

		vars = new ArrayList<List<String>>();			
		//Variables with no vertical level
		vars.add(Arrays.asList(new String[]{"Pressure", "Pressure_reduced_to_MSL"}));			
		//Variables with one vertical level
		vars.add(Arrays.asList(new String[]{"Relative_humidity_height_above_ground", "Temperature_height_above_ground"}));			
		//Variables with multiple vertical levels (hPa)
		vars.add(Arrays.asList(new String[]{"Temperature", "Relative_humidity"}));
		//No vert level and vert levels
		vars.add(Arrays.asList(new String[]{"Pressure", "Temperature", "Relative_humidity_height_above_ground"}));
		//Different vert levels
		vars.add(Arrays.asList(new String[]{"Relative_humidity_height_above_ground", "Temperature" }));
		
		projectionRect = new ArrayList<double[]>();
		
		projectionRect.add(new double[]{-4226.106971141345, -832.6983183345455, -4126.106971141345, -732.6983183345455 }); //2x2
		projectionRect.add(new double[]{-600, -600, 600, 600 });//15x16
		projectionRect.add(new double[]{-4226.106971141345, 4268.6456816654545, 3250.825028858655, 4368.6456816654545 });//2x93
		
		projectionRect.add(new double[]{-4264.248291015625, -872.8428344726562, 3293.955078125, 4409.772216796875 });//Full range
		projectionRect.add(new double[]{-4864.248291015625, -1272.8428344726562, 0, 0 });//Intersects
		
		//projectionRect.add(new double[]{-6464.248291015625, -1892.8428344726562, -4064.248291015625, -972.8428344726562 }); //DOES NOT INTERSECT
		
		latlonRect = new ArrayList<double[]>();
		latlonRect.add(new double[]{-153.5889, 11.7476, -48.5984,57.4843 });
		latlonRect.add(new double[]{-160.5889, 40.7476, -106.93, 64.65 });
		latlonRect.add(new double[]{-99.171226, 57.659579, -94.967505, 61.196857 }); //Intersects the Grid but not the declared bounding box
		
		latlonRect.add(new double[]{40.0 , -40.0, 50.0, -30.0 }); //Does not intersect
	}
	
	public static List<List<String>>  getVars(){
		return vars;
	}
	
	public static List<double[]> getProjectionRect(){
		return projectionRect;
	}
	
	public static List<double[]> getLatLonRect(){
		return latlonRect;
	}	

}
