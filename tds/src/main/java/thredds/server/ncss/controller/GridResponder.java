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
package thredds.server.ncss.controller;

import thredds.server.config.ThreddsConfig;
import thredds.server.exception.RequestTooLargeException;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.exception.OutOfBoundariesException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssGridParamsBean;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import thredds.server.ncss.view.dsg.DsgSubsetWriterFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Respond to Grid ncss requests
 */
class GridResponder {
  static private final short ESTIMATED_C0MPRESION_RATE = 5;  // Compression rate used to estimate the filesize of netcdf4 compressed files

  ///////////////////////////////////////////////////////////////////////////////
  private GridCoverageDataset gcd;
  private String requestPathInfo;
  private NcssDiskCache ncssDiskCache;

  GridResponder(GridCoverageDataset gcd, String requestPathInfo, NcssDiskCache ncssDiskCache) {
    this.gcd = gcd;
    this.requestPathInfo = requestPathInfo;
    this.ncssDiskCache = ncssDiskCache;
  }

  File getGridResponseFile(NcssGridParamsBean params, NetcdfFileWriter.Version version) throws NcssException, InvalidRangeException, ParseException, IOException {


    long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
    if (maxFileDownloadSize > 0) {
      long estimatedSize = 0; // CFGridWriter2.makeSizeEstimate(gcd, vars, bbox, projRect, horizStride, zRange, dateRange, timeStride, addLatLon);

      if (version == NetcdfFileWriter.Version.netcdf4)
        estimatedSize /= ESTIMATED_C0MPRESION_RATE;

      if (estimatedSize > maxFileDownloadSize)
        throw new RequestTooLargeException("NCSS response too large = " + estimatedSize + " max = " + maxFileDownloadSize);
    }

    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();

    String filename = NcssRequestUtils.getFileNameForResponse(requestPathInfo, version);
    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = ncssDiskCache.getDiskCache().getCacheFile(pathname);
    if (ncFile == null)
      throw new IllegalStateException("NCSS misconfigured cache = ");
    String cacheFilename = ncFile.getPath();

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, cacheFilename, null); // default chunking - let user control at some point
    GridSubset subset = params.makeSubset(gcd.getCalendar());
    CFGridCoverageWriter.writeFile(gcd, params.getVar(), subset, params.isAddLatLon(), writer);

    return new File(cacheFilename);
  }

  private Range getZRange(GridCoverageDataset gcd, Double verticalCoord, Integer vertStride, List<String> vars) throws OutOfBoundariesException {

    boolean hasVerticalCoord = false;
    Range zRange = null;

    if (verticalCoord != null) {
      hasVerticalCoord = !Double.isNaN(verticalCoord);
      // allow a vert level to be specified - but only one, and look in
      // first 3D var with nlevels > 1
      if (hasVerticalCoord) {
        try {
          for (String varName : vars) {
            GridCoverage grid = gcd.findCoverage(varName);
            GridCoordAxis vertAxis = gcd.getZAxis(gcd.findCoordSys(grid.getCoordSysName()));
            if (vertAxis != null && vertAxis.getNcoords() > 1) {
              int bestIndex = -1;
              double bestDiff = Double.MAX_VALUE;
              for (int i = 0; i < vertAxis.getNcoords(); i++) {
                double diff = Math.abs(vertAxis.getCoord(i) - verticalCoord);
                if (diff < bestDiff) {
                  bestIndex = i;
                  bestDiff = diff;
                }
              }
              if (bestIndex >= 0)
                zRange = new Range(bestIndex, bestIndex);
            }
          }
        } catch (InvalidRangeException ire) {
          throw new OutOfBoundariesException("Invalid vertical level: " + ire.getMessage());
        }
        // there's also a vertStride, but not needed since only 1D slice
        // is allowed
      }
    } else { // No vertical range was provided, we get the zRange with the zStride (1 by default)

      if (vertStride > 1) {
        try {
          zRange = new Range(0, 0, vertStride);
          for (String varName : vars) {
            GridCoverage grid = gcd.findCoverage(varName);
            GridCoordAxis vertAxis = gcd.getZAxis(gcd.findCoordSys(grid.getCoordSysName()));
            if (vertAxis != null) {
              int n = (int) vertAxis.getNcoords();
              int last = Math.max(zRange.last(), n - 1);  // LOOK bug n vs n-1 ??
              zRange = new Range(zRange.first(), zRange.last() > n ? zRange.last() : n - 1, vertStride);
            }
          }
        } catch (InvalidRangeException ire) {
          throw new OutOfBoundariesException("Invalid vertical stride: " + ire.getMessage());
        }

      }
    }

    return zRange;
  }

  File getPointResponseFile(NcssGridParamsBean params, NetcdfFileWriter.Version version)
          throws NcssException, InvalidRangeException, ParseException, IOException {

    GridDataset gridDataset = (GridDataset) fd;
    LatLonPoint point = new LatLonPointImpl(queryParams.getLatitude(), queryParams.getLongitude()); //Check if the point is within boundaries!!
    checkRequestedVars(gridDataset, queryParams);
    Map<String, List<String>> groupVars = groupVarsByVertLevels(gridDataset, queryParams);

    // LOOK - shouldnt throw exception if we can help it - just a user error
    if (!isPointWithinBoundaries(gridDataset, point, groupVars)) {
      throw new OutOfBoundariesException("Requested Lat/Lon Point (+" + point + ") is not contained in the Data. " +
              "Data Bounding Box = " + gridDataset.getBoundingBox().toString2());
    }

    List<CalendarDate> wantedDates = getRequestedDates(gridDataset, queryParams);

    //Get point, wDates, groupedVars and vertCoort from params.

    boolean allDone = false;
    List<String> vars = new ArrayList<>();
    List<String> keys = new ArrayList<>(groupVars.keySet());
    for (String key : keys) {
      vars.addAll(groupVars.get(key));
    }

    Double vertCoord = queryParams.getVertCoord();

    writer = PointDataWriterFactory.factory(format, out, diskCache);
    if (writer.header(groupVars, gridDataset, wantedDates, getTimeDimAtts(gridDataset), point, vertCoord)) {
      boolean allPointsRead = writer.write(groupVars, gridDataset, wantedDates, point, vertCoord);
      allDone = writer.trailer() && allPointsRead;
    }


    return new File(cacheFilename);
  }


}
