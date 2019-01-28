package ucar.nc2.ft2.simpgeometry;

import java.util.List;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

/**
 * Helpers for various simple geometry tasks
 * relating to the CF standard.
 * 
 * @author wchen@usgs.gov
 *
 */
public class CFSimpleGeometryHelper {

	/**
	 * Gets the subset string  to be used in NetCDFFile.read given a variable and some indicies.
	 * useful for subsetting timeseries
	 * 
	 * 
	 * @param var variable to subset
	 * @param beginInd beginning index (inclusive)
	 * @param endInd end index (exclusive)
	 * @param id The SimpleGeometryID to index
	 * @return subset string
	 */
	public static String getSubsetString(Variable var, int beginInd, int endInd, int id){
		if(var == null) return null;
		
		String subStr = "";
		
		List<Dimension> dimList = var.getDimensions();
		
		// Enforce two dimension arrays
		if(dimList.size() > 2 || dimList.size() < 1) {
			return null;
		}
		
		for(int i = 0; i < dimList.size(); i++) {
			
			Dimension dim = dimList.get(i);
			if(dim == null) continue;
			
			// If not CF Time then select only that ID
			if(!CF.TIME.equalsIgnoreCase(dim.getShortName()) && !CF.TIME.equalsIgnoreCase(dim.getFullNameEscaped())) {
				subStr += id;
			}
			
			// Otherwise subset based on time
			else {
				
				if(beginInd < 0 || endInd < 0) subStr += ":";
				else subStr += (beginInd + ":" + endInd);
			}
			
			if(i < dimList.size() - 1) {
				subStr += ",";
			}
		}
		
		return subStr;
	}
	
	/**
	 * Gets the subset string  to be used in NetCDFFile.read given a variable and an ID.
	 * useful for subsetting timeseries. This version will take the whole timeseries.
	 * 
	 * 
	 * If a time dimension is not present, however, it will default to id first and then subsetter second.
	 * 
	 * @param var variable to subset
	 * @param id The SimpleGeometryID to index
	 * @return subset string
	 */
	public static String getSubsetString(Variable var, int id) {
		return getSubsetString(var, -1, -1, id);
	}
}
