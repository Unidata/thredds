package thredds.server.ncSubset.view;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;

interface PointDataWriter {

	void setHTTPHeaders(GridDataset gds, String pathInfo, boolean isStream);
	

	boolean header(Map<String, List<String>> groupedVars, GridDataset gds, List<CalendarDate> wDates, List<Attribute> timeDimAtts, LatLonPoint point, Double vertCoord);
	
	boolean write(Map<String, List<String>> groupedVars, GridDataset gds, List<CalendarDate> wDates, LatLonPoint point, Double vertCoord) throws InvalidRangeException;
	
	boolean trailer();
	
	HttpHeaders getResponseHeaders();

	
}
