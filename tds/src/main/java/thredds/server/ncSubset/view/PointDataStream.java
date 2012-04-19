package thredds.server.ncSubset.view;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.SupportedFormat;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

public final class PointDataStream {

	static private Logger log = LoggerFactory.getLogger(PointDataStream.class);

	//private final SupportedFormat supportedFormat;
	//private final OutputStream outputStream;
	
	private final PointDataWriter pointDataWriter;

	private PointDataStream(SupportedFormat supportedFormat, OutputStream outputStream) {
		//this.supportedFormat = supportedFormat;
		//this.outputStream = outputStream;
		this.pointDataWriter = AbstractPointDataWriterFactory
				.createPointDataWriterFactory(supportedFormat)
				.createPointDataWriter(outputStream);
	}

	//private final boolean stream(GridDataset gds, LatLonPoint point, CalendarDateRange dates, List<String> vars) throws OutOfBoundariesException, DateUnitException{
	private final boolean stream(GridDataset gds, LatLonPoint point, List<CalendarDate> wDates, List<String> vars) throws OutOfBoundariesException, DateUnitException{
		
		
		boolean allDone = false;
		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds,	vars);
		//List<CalendarDate> wDates = NcssRequestUtils.wantedDates(gap, dates);
		
		//Variables have same 1D timeAxis??? 
		GridDatatype grid = gds.findGridDatatype(vars.get(0));
						
		CoordinateAxis timeAxis =grid.getCoordinateSystem().getTimeAxis();		 		 
		 //if( timeAxis instanceof CoordinateAxis1DTime  ){
		 if( CoordinateAxis1DTime.class.isAssignableFrom(timeAxis.getClass())  ){
			 timeAxis = (CoordinateAxis1DTime)timeAxis;
		 }else{
			 //This is unsupported...??
			 log.debug("Unsupported operation on grids with 2D time axis");
		 }		
		
		boolean pointRead=true;
		if(pointDataWriter.header(vars, gds, wDates,getDateUnit(grid), point)){			
			// Iterating the wanted dates				
			CalendarDate date;
			Iterator<CalendarDate> it = wDates.iterator();
			while(pointRead && it.hasNext()){
				date = it.next();
				pointRead =pointDataWriter.write(vars, gds, gap, date, point);
			}
		}
			
		allDone = pointDataWriter.trailer() && pointRead;
				
		return allDone;
	}

	//public final boolean stream(GridDataset gds, LatLonPoint point,	CalendarDateRange dates, List<String> vars, List<Double> vertCoords) throws OutOfBoundariesException, DateUnitException {
	public final boolean stream(GridDataset gds, LatLonPoint point,	List<CalendarDate> wDates, List<String> vars, List<Double> vertCoords) throws OutOfBoundariesException, DateUnitException {


		if (vertCoords.isEmpty()) return stream(gds, point, wDates, vars);
		boolean allDone = false;

		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds,	vars);
		//List<CalendarDate> wDates = NcssRequestUtils.wantedDates(gap, dates);

		// Asuming all vars have the same vertical level...(and same 1D timeAxis!!! )
		GridDatatype gridForVertLevels = gds.findGridDatatype(vars.get(0));
		CoordinateAxis1D zAxis = gridForVertLevels.getCoordinateSystem().getVerticalAxis();		
		CoordinateAxis timeAxis =gridForVertLevels.getCoordinateSystem().getTimeAxis();		 		 
		 //if( timeAxis instanceof CoordinateAxis1DTime  ){
		 if( CoordinateAxis1DTime.class.isAssignableFrom(timeAxis.getClass())  ){
			 timeAxis = (CoordinateAxis1DTime)timeAxis;
		 }else{
			 //This is unsupported...??
		 }
					
	 
		if(pointDataWriter.header(vars, gds, wDates, getDateUnit(gridForVertLevels), point, zAxis)){
			boolean pointRead=true;
			//Iterating on the requested vertical levels
			for (Double vertLevel : vertCoords) {
				// --- Find the targeted vertical level on zAxis corresponding to
				// the first grid with vertical levels ) ---
				Double targetLevel = vertLevel;
				int coordLevel = 0;
				// If zAxis has one level zAxis.findCoordElement(vertLevel) returns -1 and only works with vertLevel = 0
				// Workaround while not fixed in CoordinateAxis1D
				if (zAxis.getSize() == 1) {
					targetLevel = 0.0;
				} else {
					coordLevel = zAxis.findCoordElement(vertLevel);
					if (coordLevel > 0) {
						targetLevel = zAxis.getCoordValue(coordLevel);
					}
				}
				// Iterating the wanted dates				
				CalendarDate date;
				Iterator<CalendarDate> it = wDates.iterator();
				
				try{
					while(pointRead && it.hasNext()){
						date = it.next();
						pointRead =pointDataWriter.write(vars, gds, gap, date, point, targetLevel, zAxis.getUnitsString());
					}
				}catch(ArrayIndexOutOfBoundsException e){
					throw new OutOfBoundariesException("Requested Lat/Lon Point (+" + point + ") is not contained in the Data.", e);
				}
				
			}
			
			allDone = pointDataWriter.trailer() && pointRead;
		}			

		return allDone;
	}
	
	
	private DateUnit getDateUnit(GridDatatype grid) throws DateUnitException{
		// Asuming all vars have the same time axis and it is 1D...
		String timeUnitString = grid.getCoordinateSystem().getTimeAxis().getUnitsString();
		DateUnit du =null;
		
		try{
			du =new DateUnit(timeUnitString);
		}catch(Exception e){
			throw new DateUnitException("Error creating DateUnits for station", e);
		}
		
		return du;
		
	}
	
	public final HttpHeaders getHttpHeaders(){
		
		return pointDataWriter.getResponseHeaders();
	}
	
	public static final PointDataStream createPointDataStream(SupportedFormat supportedFormat, OutputStream outputStream){
		
		return new PointDataStream(supportedFormat, outputStream);
		
	} 
}
