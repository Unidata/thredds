/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.view.gridaspoint;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncss.exception.DateUnitException;
import thredds.server.ncss.exception.UnsupportedOperationException;
import thredds.server.ncss.format.SupportedFormat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * merged into GridAsPointResponder.
 * left (fo now) for unit testing
 */
public final class PointDataStream {
  static private Logger log = LoggerFactory.getLogger(PointDataStream.class);

  public static PointDataStream factory(SupportedFormat supportedFormat, OutputStream outputStream, DiskCache2 diskCache){
 		return new PointDataStream(supportedFormat, outputStream, diskCache);
 	}

	private final PointDataWriter pointDataWriter;

	private PointDataStream(SupportedFormat supportedFormat, OutputStream outputStream, DiskCache2 diskCache) {
		this.pointDataWriter = PointDataWriterFactory.factory(supportedFormat, outputStream, diskCache);
	}

	public final boolean stream(GridDataset gds, LatLonPoint point,	List<CalendarDate> wDates, Map<String, List<String>> groupedVars, Double vertCoord) throws DateUnitException, UnsupportedOperationException, InvalidRangeException {
		
		boolean allDone= false;
		List<String> vars = new ArrayList<>();
		List<String> keys = new ArrayList<>(groupedVars.keySet());
		for(String key : keys){			
			vars.addAll(groupedVars.get(key));
		}
					
		//if(pointDataWriter.header(groupedVars, gds, wDates, getDateUnit(gds) , point, vertCoord)){
		if (pointDataWriter.header(groupedVars, gds, wDates, getTimeDimAtts(gds) , point, vertCoord)){
			boolean allPointsRead = false;
			allPointsRead = pointDataWriter.write(groupedVars, gds, wDates, point, vertCoord);
			allDone = pointDataWriter.trailer() && allPointsRead;
		}
		return allDone;
	}
	
	
	private List<Attribute> getTimeDimAtts(GridDataset gds){
		
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

		List<Attribute> timeAtts = new ArrayList<>();
		
		String timeUnitString = tAxis.getUnitsString();
		if( tAxis.getDataType() == DataType.STRING && tAxis.getUnitsString().equals("") ){ //Time axis contains String dates (ISO ??)			
			CalendarDate startDate = tAxis.getCalendarDate(0);
			timeUnitString = "seconds since "+ startDate.toString(); //Units will be seconds since the origin of the time axis
			timeAtts.add(new Attribute( CDM.UNITS, timeUnitString ));
		}else{
			Attribute tUnits = tAxis.findAttribute(CDM.UNITS);
			if(tUnits != null )
				timeAtts.add( tUnits );
		}
		//Check calendar
		Attribute tCal = tAxis.findAttribute( CF.CALENDAR );
		if(tCal != null){
			timeAtts.add(tCal);
		}
		//Chek names..
		Attribute tStdName = tAxis.findAttribute( CF.STANDARD_NAME );
		if(tStdName != null){
			timeAtts.add(tStdName);
		}		
		Attribute tLongName = tAxis.findAttribute( CDM.LONG_NAME );
		if(tLongName != null){
			timeAtts.add(tLongName);
		}		
		
		return timeAtts;		
	}
	
	public final HttpHeaders getHttpHeaders(GridDataset gds, String pathInfo, Boolean isStream){
		pointDataWriter.setHTTPHeaders(gds, pathInfo, isStream);
		return pointDataWriter.getResponseHeaders();
	}

}
