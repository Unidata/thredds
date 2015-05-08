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
package thredds.server.ncss.view.gridaspoint.gridaspoint;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.OutOfBoundariesException;
import thredds.server.ncss.exception.VariableNotContainedInDatasetException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
//import thredds.server.ncss.view.gridaspoint.GridAsPointWriter;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.grid.GridCoordAxis;
import ucar.nc2.ft2.coverage.grid.GridCoverage;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Respond to GridAsPoint request
 *
 */
public class GridAsPointResponder {
  private final GridCoverageDataset gcd;
  private final GridAsPointWriter writer;
  private final Map<String, List<String>> groupVarsByVertLevels;

  GridAsPointResponder(GridCoverageDataset gcd, NcssParamsBean params, NcssDiskCache ncssDiskCache, SupportedFormat format, OutputStream out) {
    this.gcd = gcd;
    groupVarsByVertLevels = groupVarsByVertLevels(gcd, params);
    writer = GridAsPointWriter.factory(gcd, format, out, ncssDiskCache);
  }

  public void respond(NcssParamsBean params) throws VariableNotContainedInDatasetException, OutOfBoundariesException, ParseException, InvalidRangeException {

    LatLonPoint latlon = new LatLonPointImpl(params.getLatitude(), params.getLongitude());
    Double vertCoord = params.getVertCoord();

    if (writer.header(groupVarsByVertLevels, params.getCalendarDateRange(null), getTimeDimAtts(), latlon, vertCoord)) {
      boolean allPointsRead = writer.write(groupVarsByVertLevels, params.getCalendarDateRange(null), latlon, vertCoord);
      writer.trailer();
    }
  }

  public HttpHeaders getResponseHeaders(GridCoverageDataset gds, SupportedFormat format, String datasetPath) {
    writer.setHTTPHeaders(datasetPath, format.isStream());
    return writer.getResponseHeaders();
  }

  private List<Attribute> getTimeDimAtts() {
    return new ArrayList<>();    // LOOK
  }


  public static final String no_vert_levels = "no_vert_level";

  /**
   * for all requested variables (in params), group by vertical axis name
   * "no_vert_level" when 2D
   */
  protected Map<String, List<String>> groupVarsByVertLevels(GridCoverageDataset gcd, NcssParamsBean params) {
    List<String> vars = params.getVar();
    Map<String, List<String>> result = new HashMap<>();

    for (String varName : vars) {
      GridCoverage grid = gcd.findCoverage(varName);
      GridCoordAxis vertAxis = gcd.getZAxis(gcd.findCoordSys(grid.getCoordSysName()));

      String axisKey;
      if (vertAxis == null) {
        axisKey = no_vert_levels;
      } else {
        axisKey = vertAxis.getName();
      }

      if (result.containsKey(axisKey)) {
        result.get(axisKey).add(varName);
      } else {
        List<String> varListForVerlLevel = new ArrayList<>();
        varListForVerlLevel.add(varName);
        result.put(axisKey, varListForVerlLevel);
      }
    }

    return result;
  }
}

  /*

 		//If the grid does not have time axis, return null
 		//if(grid.getCoordinateSystem().getTimeAxis() == null)
 		//	return null;
 		CoordinateAxis1DTime tAxis = null;
 		List<ucar.nc2.dt.GridDataset.Gridset> ggss = gcd.getGridsets();

 		Iterator<ucar.nc2.dt.GridDataset.Gridset> it = ggss.iterator();
 		while( tAxis == null && it.hasNext() ){
 			ucar.nc2.dt.GridDataset.Gridset gs = it.next();
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
 		}                                                                                                 `
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
 	}  */

 /*
	private boolean isPointWithinBoundaries(LatLonPoint point, Map<String, List<String>> groupVars){
		//LatLonRect bbox = gds.getBoundingBox();
		boolean isInData = true;
		List<String> keys = new ArrayList<>(groupVars.keySet());

		int[] xy;
		Iterator<String> it = keys.iterator();

		while( it.hasNext() && isInData ){
			String key = it.next();
			GridDatatype grid = gridDataset.findGridDatatype(groupVars.get(key).get(0));
			GridCoordSystem coordSys = grid.getCoordinateSystem();
			ProjectionPoint p = coordSys.getProjection().latLonToProj(point);
			xy = coordSys.findXYindexFromCoord(p.getX(), p.getY(), null);

			if(xy[0] < 0 || xy[1] < 0  ){
				isInData = false;
			}
		}

		return isInData;
	}   */

	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.NCSSPointDataStream#setResponseHeaders(ucar.nc2.constants.FeatureType, thredds.server.ncSubset.format.SupportedFormat, java.lang.String)
	 */


  /*
	public static GridAsPointDataset buildGridAsPointDataset(GridCoverageDataset gcd, List<String> vars) {

   List<GridCoverage> grids = new ArrayList<>();
   for (String gridName : vars) {
     GridCoverage grid = gcd.findCoverage(gridName);
     if (grid != null)
       grids.add(grid);
   }
   return new GridAsPointDataset(grids);
 }

 public static List<CalendarDate> wantedDates(GridAsPointDataset gap, CalendarDateRange dates, long timeWindow)
         throws TimeOutOfWindowException, OutOfBoundariesException {

   CalendarDate start = dates.getStart();
   CalendarDate end = dates.getEnd();
   List<CalendarDate> gdsDates = gap.getDates();

   if (start.isAfter(gdsDates.get(gdsDates.size() - 1)) || end.isBefore(gdsDates.get(0))) {
     throw new OutOfBoundariesException("Requested time range does not intersect the Data Time Range = " +
             gdsDates.get(0) + " to " + gdsDates.get(gdsDates.size() - 1));
   }

   List<CalendarDate> wantDates = new ArrayList<>();

   if (dates.isPoint()) {
     int best_index = 0;
     long best_diff = Long.MAX_VALUE;
     for (int i = 0; i < gdsDates.size(); i++) {
       CalendarDate date = gdsDates.get(i);
       long diff = Math.abs(date.getDifferenceInMsecs(start));
       if (diff < best_diff) {
         best_index = i;
         best_diff = diff;
       }
     }
     if (timeWindow > 0 && best_diff > timeWindow) { //Best time is out of our acceptable timeWindow
       throw new TimeOutOfWindowException("There is not time within the provided time window");
     }
     wantDates.add(gdsDates.get(best_index));

   } else {
     for (CalendarDate date : gdsDates) {
       boolean tooEarly = date.isBefore(start);
       boolean tooLate = date.isAfter(end);
       if (tooEarly || tooLate)
         continue;
       wantDates.add(date);
     }
   }
   return wantDates;
 }


 public static List<VariableSimpleIF> wantedVars2VariableSimple(List<String> wantedVars, GridCoverageDataset gcd, NetcdfDataset ncfile) {

   // need VariableSimpleIF for each variable
   List<VariableSimpleIF> varList = new ArrayList<>(wantedVars.size());

   //And wantedVars must be in the dataset
   for (String var : wantedVars) {
     VariableEnhanced ve = gcd.findGridDatatype(var).getVariable();

     //List<Dimension> lDims =ve.getDimensions();
     //StringBuilder dims = new StringBuilder("");
     //for(Dimension d: lDims){
     //	dims.append(" ").append(d.getName());
     //}
     String dims = ""; // always scalar ????

     VariableSimpleIF want = new VariableDS(ncfile, null, null, ve.getShortName(), ve.getDataType(), dims,
             ve.getUnitsString(), ve.getDescription());

     varList.add(want);
   }

   return varList;
 }


 public static GridDatatype getTimeGrid(Map<String, List<String>> groupedVars, GridCoverageDataset gcd) {

   List<String> keys = new ArrayList<>(groupedVars.keySet());
   GridDatatype timeGrid = null;
   List<String> allVars = new ArrayList<>();
   for (String key : keys) {
     allVars.addAll(groupedVars.get(key));
   }

   Iterator<String> it = allVars.iterator();

   while (timeGrid == null && it.hasNext()) {
     String var = it.next();
     if (gridDataset.findGridDatatype(var).getCoordinateSystem().hasTimeAxis()) {
       timeGrid = gridDataset.findGridDatatype(var);
     }
   }
   ///
   return timeGrid;
 }

 public static Double getTimeCoordValue(GridCoverage grid, CalendarDate date, CalendarDate origin) {

   CoordinateAxis1DTime tAxis = grid.getCoordinateSystem().getTimeAxis1D();

   if (tAxis == null) {
     return -1.0;
   }

   Integer wIndex = tAxis.findTimeIndexFromCalendarDate(date);
   Double coordVal;

   //Check axis dataType --> Time axis for some collections (joinExistingOne) is String
   //In that case we use the seconds since the origin of the time axis as unit
   if (tAxis.getDataType() == DataType.STRING) {
     CalendarDate wanted = tAxis.getCalendarDate(wIndex);
     coordVal = (double) wanted.getDifferenceInMsecs(origin) / 1000;

   } else {
     coordVal = tAxis.getCoordValue(wIndex);
   }

   return coordVal;
 }

 public static Double getTargetLevelForVertCoord(CoordinateAxis1D zAxis, Double vertLevel) {

   Double targetLevel = vertLevel;
   int coordLevel;
   // If zAxis has one level zAxis.findCoordElement(vertLevel) returns -1 and only works with vertLevel = 0
   // Workaround while not fixed in CoordinateAxis1D
   if (zAxis.getSize() == 1) {
     targetLevel = 0.0;
   } else {
     //coordLevel = zAxis.findCoordElement(vertLevel);
     coordLevel = zAxis.findCoordElementBounded(vertLevel);

     if (coordLevel > 0) {
       targetLevel = zAxis.getCoordValue(coordLevel);
     }
   }

   return targetLevel;
 }


} */
