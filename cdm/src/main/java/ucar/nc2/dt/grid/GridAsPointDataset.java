/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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

    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null);

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
