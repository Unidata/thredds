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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import thredds.server.ncss.controller.NcssDiskCache;
import thredds.server.ncss.format.SupportedFormat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Write GridAsPoint
 */

public abstract class GridAsPointWriter {

  static public GridAsPointWriter factory(GridCoverageDataset gds, SupportedFormat wantFormat, OutputStream outputStream, NcssDiskCache ncssDiskCache) {

    if (wantFormat == SupportedFormat.XML_FILE || wantFormat == SupportedFormat.XML_STREAM) {
      return new XMLGridAsPointWriter(gds, outputStream);
    }

    if (wantFormat == SupportedFormat.NETCDF3) {
      return new NetCDFGridAsPointWriter(gds, NetcdfFileWriter.Version.netcdf3, outputStream, ncssDiskCache);
    }

    if (wantFormat == SupportedFormat.NETCDF4) {
      return new NetCDFGridAsPointWriter(gds, NetcdfFileWriter.Version.netcdf4, outputStream, ncssDiskCache);
    }

    if (wantFormat == SupportedFormat.CSV_FILE || wantFormat == SupportedFormat.CSV_STREAM) {
      return new CSVGridAsPointWriter(gds, outputStream);
    }

    throw new IllegalStateException("PointDataWriter does not support " + wantFormat);
  }


  public abstract void setHTTPHeaders(String pathInfo, boolean isStream);

  public abstract boolean header(Map<String, List<String>> groupVarsByVertLevels, CalendarDateRange calendarDateRange, List<Attribute> timeDimAtts, LatLonPoint point, Double vertCoord);

  public abstract boolean write(Map<String, List<String>> groupVarsByVertLevels, CalendarDateRange calendarDateRange, LatLonPoint point, Double vertCoord) throws InvalidRangeException, IOException;

  public abstract boolean trailer();

  public abstract HttpHeaders getResponseHeaders();

  /**
   * Returns the actual vertical level if the grid has vertical transformation or -9999.9 otherwise
   */
  public double getActualVertLevel(GridCoverageDataset gcd, GridCoverage grid, CalendarDate date, LatLonPoint point, double targetLevel) { //} throws IOException, InvalidRangeException {

    double actualLevel = -9999.9;
    return actualLevel;
  }

      /*
    }

    //Check vertical transformations for the grid
    GridCoordSys gcs = gcd.findCoordSys(grid.getCoordSysName());
    GridCoordAxis vertAxis = gcd.getZAxis(gcs);

    GridCoordTransform vt = gcd.getVerticalTransform(grid);

    if (vertAxis != null) {
      int[] result = new int[2];
      gcs.findXYindexFromLatLon(point.getLatitude(), point.getLongitude(), result);
      CoordinateAxis1DTime timeAxis = cs.getTimeAxis1D();
      int vertCoord = vertAxis.findCoordElement(targetLevel);

      int timeIndex = 0;
      if (timeAxis != null) {
        timeIndex = timeAxis.findTimeIndexFromCalendarDate(date);

      } //If null timAxis might be 2D -> not supported (handle this)

      ArrayDouble.D1 actualLevels = vt.getCoordinateArray1D(timeIndex, result[0], result[1]);
      actualLevel = actualLevels.get(vertCoord);
    }

    return actualLevel;
  }   */
}
