package thredds.server.ncSubset.view;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.format.SupportedFormat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

public final class PointDataStream {

	static private Logger log = LoggerFactory.getLogger(PointDataStream.class);

	private final PointDataWriter pointDataWriter;

	private PointDataStream(SupportedFormat supportedFormat, OutputStream outputStream) {
		
		this.pointDataWriter = AbstractPointDataWriterFactory
				.createPointDataWriterFactory(supportedFormat)
				.createPointDataWriter(outputStream);
	}

	public final boolean stream(GridDataset gds, LatLonPoint point,	List<CalendarDate> wDates, Map<String, List<String>> groupedVars, Double vertCoord) throws DateUnitException, UnsupportedOperationException, InvalidRangeException {
		
		boolean allDone= false;
		List<String> vars = new ArrayList<String>();
		List<String> keys = new ArrayList<String>(groupedVars.keySet());
		for(String key : keys){			
			vars.addAll(groupedVars.get(key));

		}				
				
		//Assuming all variables have same time dimension!!!			
		GridDatatype gridForTimeUnits= gds.findGridDatatype(vars.get(0));
		
		if(pointDataWriter.header(groupedVars, gds, wDates, getDateUnit(gridForTimeUnits) , point, vertCoord)){ 
			
			boolean allPointsRead = false;
			allPointsRead = pointDataWriter.write(groupedVars, gds, wDates, point, vertCoord);
			allDone = pointDataWriter.trailer() && allPointsRead;
		}
		return allDone;
	}
	
	private DateUnit getDateUnit(GridDatatype grid) throws DateUnitException{

		//If the grid does not have time axis, return null
		if(grid.getCoordinateSystem().getTimeAxis() == null)
			return null;

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
	
	
	public final HttpHeaders getHttpHeaders(GridDataset gds, String pathInfo){
		
		pointDataWriter.setHTTPHeaders(gds, pathInfo);
		return pointDataWriter.getResponseHeaders();
	}
	
	public static final PointDataStream createPointDataStream(SupportedFormat supportedFormat, OutputStream outputStream){
		
		return new PointDataStream(supportedFormat, outputStream);
		
	} 
}
