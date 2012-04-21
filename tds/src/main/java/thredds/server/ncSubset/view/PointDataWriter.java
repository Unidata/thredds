package thredds.server.ncSubset.view;

import java.util.List;

import org.springframework.http.HttpHeaders;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

interface PointDataWriter {


	boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit, LatLonPoint point, CoordinateAxis1D zAxis);
	
	boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit, LatLonPoint point);
	
	boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point, Double targetLevel, String zUnits);
	
	boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point);
	
	boolean trailer();
	
	HttpHeaders getResponseHeaders();
}
