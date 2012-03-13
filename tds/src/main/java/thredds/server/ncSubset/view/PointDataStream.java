package thredds.server.ncSubset.view;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.SupportedFormat;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;

public final class PointDataStream {

	static private Logger log = LoggerFactory.getLogger(PointDataStream.class);

	private final SupportedFormat supportedFormat;
	private final OutputStream outputStream;
	
	private final PointDataWriter pointDataWriter;

	public PointDataStream(SupportedFormat supportedFormat, OutputStream outputStream) {
		this.supportedFormat = supportedFormat;
		this.outputStream = outputStream;
		this.pointDataWriter = AbstractPointDataWriterFactory
				.createPointDataWriterFactory(supportedFormat)
				.createPointDataWriter(outputStream);
	}

	private final boolean stream(GridDataset gds, LatLonPoint point, CalendarDateRange dates, List<String> vars) throws OutOfBoundariesException{
		
		
		boolean allDone = false;
		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds,	vars);
		List<CalendarDate> wDates = NcssRequestUtils.wantedDates(gap, dates);
		
		boolean pointRead=true;
		if(pointDataWriter.header(vars, gds, wDates, point)){			
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

	public final boolean stream(GridDataset gds, LatLonPoint point,	CalendarDateRange dates, List<String> vars, List<Double> vertCoords) throws OutOfBoundariesException{


		if (vertCoords.isEmpty()) return stream(gds, point, dates, vars);
		boolean allDone = false;

		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds,	vars);
		List<CalendarDate> wDates = NcssRequestUtils.wantedDates(gap, dates);

		// Asuming all vars have the same vertical level...
		GridDatatype gridForVertLevels = gds.findGridDatatype(vars.get(0));
		CoordinateAxis1D zAxis = gridForVertLevels.getCoordinateSystem().getVerticalAxis();

		if(pointDataWriter.header(vars, gds, wDates, point, zAxis)){
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
				while(pointRead && it.hasNext()){
					date = it.next();
					pointRead =pointDataWriter.write(vars, gds, gap, date, point, targetLevel, zAxis.getUnitsString());
				}
			}
			
			allDone = pointDataWriter.trailer() && pointRead;
		}			

		return allDone;
	}
	
	public final HttpHeaders getHttpHeaders(){
		
		return pointDataWriter.getResponseHeaders();
	}
}
