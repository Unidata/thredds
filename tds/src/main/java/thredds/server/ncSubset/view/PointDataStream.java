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
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.format.SupportedFormat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
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
		//GridDatatype gridForTimeUnits= gds.findGridDatatype(vars.get(0));
		
		if(pointDataWriter.header(groupedVars, gds, wDates, getDateUnit(gds) , point, vertCoord)){ 
			
			boolean allPointsRead = false;
			allPointsRead = pointDataWriter.write(groupedVars, gds, wDates, point, vertCoord);
			allDone = pointDataWriter.trailer() && allPointsRead;
		}
		return allDone;
	}
	
	
	private DateUnit getDateUnit(GridDataset gds) throws DateUnitException{

		//If the grid does not have time axis, return null
		//if(grid.getCoordinateSystem().getTimeAxis() == null)
		//	return null;
		CoordinateAxis1DTime tAxis = null;
		List<Gridset> ggss = gds.getGridsets();
		
		Iterator<Gridset> it = ggss.iterator();
		while( tAxis == null && it.hasNext() ){
			Gridset gs = it.next();
			tAxis = gs.getGeoCoordSystem().getTimeAxis1D();
		}
		
		if(tAxis == null) return null;

		String timeUnitString = tAxis.getUnitsString();
		if( tAxis.getDataType() == DataType.STRING && tAxis.getUnitsString().equals("") ){ //Time axis contains String dates (ISO ??)
			
			CalendarDate startDate = tAxis.getCalendarDate(0);
			timeUnitString = "seconds since "+ startDate.toString(); //Units will be seconds since the origin of the time axis 
						
		}
							
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
