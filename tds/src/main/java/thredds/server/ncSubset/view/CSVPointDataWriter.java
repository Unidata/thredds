package thredds.server.ncSubset.view;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.jfree.util.Log;
import org.springframework.http.HttpHeaders;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

class CSVPointDataWriter implements PointDataWriter {

	private PrintWriter printWriter;
	
	private CSVPointDataWriter(OutputStream os){
		printWriter= new PrintWriter(os);
	}
	
	public static CSVPointDataWriter createCSVPointDataWriter(OutputStream os){
		
		return new CSVPointDataWriter(os);
	}
	
	@Override
	public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point, CoordinateAxis1D zAxis) {
		
		boolean headerWritten=false;		
		//StringBuilder sb = new StringBuilder();
		StringBuilder sb = new StringBuilder();		
		sb.append("date,");
		sb.append("lat[unit=\"degrees_north\"],");
		sb.append("lon[unit=\"degrees_east\"],");
		sb.append("vertCoord[unit=\""+zAxis.getUnitsString() +"\"],");
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
	}

	@Override
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
	}

	@Override
	public boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point,	Double targetLevel, String zUnits) {
		
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

	@Override
	public boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point) {
		
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
		return new HttpHeaders();
	}

}
