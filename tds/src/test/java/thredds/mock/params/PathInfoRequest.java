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

import java.util.Arrays;
import java.util.List;

/**
 * 
 * Provide params for parameterized tests.
 * Params match with catalogs in catalog.xml in src/test/resources/content/thredds/catalog.xml 
 * 
 * @author mhermida
 *
 */
public final class PathInfoRequest {
	
	/**
	 * List that contains one pathInfo for each of the datasets in catalog.xml
	 */
	private static List<String> pathInfo;
	
	private PathInfoRequest(){}
	
	static{
		
		pathInfo =Arrays.asList( new String[]{
				"unitTests/GFS/CONUS_80km/GFS_CONUS_80km_20120419_0000.nc",//single dataset
				"testTdsScan/GFS_CONUS_80km_20120419_0000.nc", //datasetScan 
				"testFeatureCollection/Test_Feature_Collection_fmrc.ncd", //FeatureCollection --> TwoD
				"testFeatureCollection/Test_Feature_Collection_best.ncd", //FeatureCollection --> Best
				"testFeatureCollection/files/GFS_CONUS_80km_20120418_1200.nc", //FeatureCollection --> Files
				"testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z" //FeatureCollection --> Runs
		});		
	}
	
	public static final List<String> getPatInfo(){
		
		return pathInfo;
	}
	
	public static final List<String[]> getPathInfoAsListOfArrays(){

		String[][] listStr = new String[pathInfo.size()][1];
		
		for(int i=0; i < pathInfo.size(); i++  ){
			listStr[i] = new String[]{ pathInfo.get(i) };
		}
		
		return Arrays.asList(listStr);
		
	}

}
