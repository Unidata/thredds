/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.grid;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.ma2.Array;
import ucar.unidata.geoloc.LatLonPoint;

import java.util.*;

/**
 * Add Point operations to a GridDataset.
 *
 * @author caron
 */
public class GridAsPointDataset {
  private List<GridDatatype> grids;
  private List<Date> dates;

  public GridAsPointDataset( List<GridDatatype> grids) {
    this.grids = grids;

    HashSet<Date> dateHash = new HashSet<Date>();
    List<CoordinateAxis1DTime> timeAxes = new ArrayList<CoordinateAxis1DTime>();

    for (GridDatatype grid : grids) {
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
      if ((timeAxis != null) && !timeAxes.contains(timeAxis)) {
        timeAxes.add(timeAxis);

        Date[] timeDates = timeAxis.getTimeDates();
        for (Date timeDate : timeDates)
          dateHash.add(timeDate);
      }
    }
    dates = Arrays.asList( dateHash.toArray(new Date[dateHash.size()]));
    Collections.sort(dates);
  }

  public List<Date> getDates() { return dates; }

  public boolean hasTime( GridDatatype grid, Date date) {
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    return (timeAxis != null) && timeAxis.hasTime( date);
  }

  public double getMissingValue(GridDatatype grid) {
    return Double.NaN;
    //VariableEnhanced ve = grid.getVariable();
    //return ve.getValidMax(); // LOOK bogus
  }

  public Point readData(GridDatatype grid, Date date, double lat, double lon)  throws java.io.IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    int tidx = timeAxis.findTimeIndexFromDate( date);

    int[] xy = gcs.findXYindexFromLatLonBounded(lat, lon, null);

    Array data  = grid.readDataSlice(tidx, -1, xy[1], xy[0]);

    // use actual grid midpoint
    LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);

    Point p = new Point();
    p.lat = latlon.getLatitude();
    p.lon = latlon.getLongitude();
    p.dataValue = data.getDouble( data.getIndex());
    return p;
  }

  public boolean hasVert( GridDatatype grid, double zCoord) {
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
    if (zAxis == null) return false;
    return (zAxis.findCoordElement( zCoord) >= 0);
  }

  public Point readData(GridDatatype grid, Date date, double zCoord, double lat, double lon)  throws java.io.IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    int tidx = timeAxis.findTimeIndexFromDate( date);

    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
    int zidx = zAxis.findCoordElement( zCoord);

    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null);

    Array data  = grid.readDataSlice(tidx, zidx, xy[1], xy[0]);

    // use actual grid midpoint
    LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);

    Point p = new Point();
    p.lat = latlon.getLatitude();
    p.lon = latlon.getLongitude();
    p.z = zAxis.getCoordValue( zidx);
    p.dataValue = data.getDouble( data.getIndex());
    return p;
  }


  public class Point {
    public double lat,lon,z,dataValue;
  }

}
