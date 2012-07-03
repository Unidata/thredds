package thredds.server.ncSubset.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.util.Log;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.AbstractNcssController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;

class CSVPointDataWriter implements PointDataWriter {

	private PrintWriter printWriter;
	
	private List<String> allVars; 
	
	private boolean headersSet = false;

	
	private HttpHeaders httpHeaders;
	
	private CSVPointDataWriter(OutputStream os){
		printWriter= new PrintWriter(os);
	}
	
	public static CSVPointDataWriter createCSVPointDataWriter(OutputStream os){
		
		return new CSVPointDataWriter(os);
	}
	
	//@Override
	//public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point, CoordinateAxis1D zAxis) {
	public boolean header(Map<String,List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point) {
		
		/*allVars = new ArrayList<String>();
	
		boolean headerWritten=false;		
		StringBuilder sb = new StringBuilder();		
		sb.append("date,");
		sb.append("lat[unit=\"degrees_north\"],");
		sb.append("lon[unit=\"degrees_east\"],");
		
		List<String> keys = new ArrayList<String>(groupedVars.keySet());
		for(String key : keys){
			List<String> groupVars = groupedVars.get(key);
			allVars.addAll(groupVars);			
		    CoordinateAxis1D zAxis = gridDataset.findGridDatatype(groupVars.get(0)).getCoordinateSystem().getVerticalAxis();
			//multiple vertical axis are possible!!!
		    if(zAxis != null)
		    	sb.append("vertCoord[unit=\""+zAxis.getUnitsString() +"\"],");
		
		    Iterator<String> it = groupVars.iterator();
		    while(it.hasNext()){
		    	GridDatatype grid = gridDataset.findGridDatatype(it.next());
		    	sb.append(grid.getName());			
		    	if( grid.getUnitsString()!=null ) sb.append("[unit=\"" + grid.getUnitsString() + "\"]");			
		    	if(it.hasNext()) sb.append(",");
		    }
						
		}
		printWriter.write(sb.toString());
		printWriter.println();
		headerWritten=true;
		
		return headerWritten;*/
		
		//Back to the restriction with only variables with same vertical level!!! --> only one header
		List<String> keys =new ArrayList<String>(groupedVars.keySet());
		List<String> varsGroup = groupedVars.get(keys.get(0));
		writeGroupHeader(varsGroup, gridDataset);						
		return true; //Does nothing
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel) {
		
		boolean allDone = true;
		
		List<String> keys =new ArrayList<String>(groupedVars.keySet());
		//loop over variable groups
		for(String key : keys){

			List<String> varsGroup = groupedVars.get(key);
			//writeGroupHeader(varsGroup, gridDataset);
			
			GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset,	varsGroup);			
			CoordinateAxis1D verticalAxisForGroup = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getVerticalAxis();
			if(verticalAxisForGroup ==null){
				//Read and write vars--> time, point
				allDone = allDone && write(varsGroup, gridDataset, gap, date, point);
			}else{
				//read and write time, verCoord for each variable in group
				if(targetLevel != null){
					Double vertCoord = NcssRequestUtils.getTargetLevelForVertCoord(verticalAxisForGroup, targetLevel);
					allDone = write(varsGroup, gridDataset, gap, date, point, vertCoord, verticalAxisForGroup.getUnitsString() );
				}else{//All levels
					for(Double vertCoord : verticalAxisForGroup.getCoordValues() ){
						/////Fix axis!!!!
						if(verticalAxisForGroup.getCoordValues().length ==1  )
							vertCoord =NcssRequestUtils.getTargetLevelForVertCoord(verticalAxisForGroup, vertCoord);
						
						allDone = allDone && write(varsGroup, gridDataset, gap, date, point, vertCoord, verticalAxisForGroup.getUnitsString() );
						
					}
				}				
				
			}			
			//printWriter.println();
		}
		
		return allDone;
	}	

	private void writeGroupHeader(List<String> varGroup, GridDataset gridDataset){
		
		StringBuilder sb = new StringBuilder();		
		sb.append("date,");
		sb.append("lat[unit=\"degrees_north\"],");
		sb.append("lon[unit=\"degrees_east\"],");
		
	    CoordinateAxis1D zAxis = gridDataset.findGridDatatype(varGroup.get(0)).getCoordinateSystem().getVerticalAxis();
		
	    if(zAxis != null)
	    	sb.append("vertCoord[unit=\""+zAxis.getUnitsString() +"\"],");
		
	    Iterator<String> it = varGroup.iterator();
		while(it.hasNext()){
			GridDatatype grid = gridDataset.findGridDatatype(it.next());
		    sb.append(grid.getName());			
		    if( grid.getUnitsString()!=null ) sb.append("[unit=\"" + grid.getUnitsString() + "\"]");			
		    if(it.hasNext()) sb.append(",");
		}
						
		printWriter.write(sb.toString());
		printWriter.println();					
	}
	
	/*@Override
	public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point) {
		
		boolean headerWritten=false;		
		StringBuilder sb = new StringBuilder();
		sb.append("date,");
		sb.append("lat[unit=\"degrees_north\"],");
		sb.append("lon[unit=\"degrees_east\"],");
		Iterator<String> it = vars.iterator();
		while(it.hasNext()){
			GridDatatype grid = gridDataset.findGridDatatype(it.next());
			sb.append(grid.getName());			
			if( grid.getUnitsString()!=null ) sb.append("[unit=\"" + grid.getUnitsString() + "\"]");			
			if(it.hasNext()) sb.append(",");
		}
		
		printWriter.write(sb.toString());
		printWriter.println();
		headerWritten=true;
		
		return headerWritten;
	}*/

	private boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point,	Double targetLevel, String zUnits) {
		
		boolean allDone = false;
		printWriter.write(date.toString()+",");
		int contVars= 0;					 
		Iterator<String> itVars = vars.iterator();
		try{
			while (itVars.hasNext()) {
				GridDatatype grid = gridDataset.findGridDatatype(itVars.next());
				if ( gap.hasTime(grid, date) && gap.hasVert(grid, targetLevel) ) {
					GridAsPointDataset.Point p = gap.readData(grid, date, targetLevel, point.getLatitude(), point.getLongitude());
					if(contVars == 0){
						printWriter.write(Double.valueOf(p.lat).toString()+"," );
						printWriter.write(Double.valueOf(p.lon).toString()+"," );
						printWriter.write(Double.valueOf(p.z).toString()+"," );
					}							
					printWriter.write(Double.valueOf(p.dataValue).toString());
					if(itVars.hasNext()) printWriter.write(",");
				
				} else {
					// write missingvalues!!!
					if(contVars==0){
						printWriter.write( point.getLatitude()+"," );
						printWriter.write( point.getLongitude() +"," );
						printWriter.write( targetLevel +"," );
					}
					printWriter.write( Double.valueOf(gap.getMissingValue(grid)).toString() );
				}					
				contVars++;
			}
			allDone = true;
		}catch(IOException ioe){
			Log.error("Error reading data", ioe);
		}	
		printWriter.println();	
		return allDone;
		
	}


	private boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point) {
		
		boolean allDone = false;
		printWriter.write(date.toString()+",");
		int contVars= 0;					 
		Iterator<String> itVars = vars.iterator();
		try{
			while (itVars.hasNext()) {
				GridDatatype grid = gridDataset.findGridDatatype(itVars.next());
				if (gap.hasTime(grid, date) ) {
					GridAsPointDataset.Point p = gap.readData(grid, date, point.getLatitude(), point.getLongitude());
					if(contVars == 0){
						printWriter.write(Double.valueOf(p.lat).toString()+"," );
						printWriter.write(Double.valueOf(p.lon).toString()+"," );
					}							
					printWriter.write(Double.valueOf(p.dataValue).toString());
					if(itVars.hasNext()) printWriter.write(",");
				
				} else {
					// write missingvalues!!!
					if(contVars==0){
						printWriter.write( point.getLatitude()+"," );
						printWriter.write( point.getLongitude() +"," );
					}
					printWriter.write( Double.valueOf(gap.getMissingValue(grid)).toString() );
				}					
				contVars++;
			}
			allDone = true;
		}catch(IOException ioe){
			Log.error("Error reading data", ioe);
		}	
		printWriter.println();	
		return allDone;
		
	}

	@Override
	public boolean trailer() {

		printWriter.flush();
		return true;
	}
	
	@Override
	public HttpHeaders getResponseHeaders(){
				
		return httpHeaders;
	}
	
	@Override
	public void setHTTPHeaders(GridDataset gridDataset){

		if(!headersSet){
			httpHeaders = new HttpHeaders();
			//Set the response headers...
			String filename = gridDataset.getLocationURI();
			int pos = filename.lastIndexOf("/");
			filename = filename.substring(pos + 1);
			if (!filename.endsWith(".csv"))
				filename = filename + ".csv";
			
			httpHeaders.set("Content-Location", filename );
			httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			headersSet = true;
		}	
	}	

}
