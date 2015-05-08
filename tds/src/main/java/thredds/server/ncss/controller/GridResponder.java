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

import thredds.server.exception.RequestTooLargeException;
import thredds.server.ncss.exception.*;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.config.ThreddsConfig;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.CFGridWriter2;
import ucar.nc2.ft2.coverage.grid.GridCoordAxis;
import ucar.nc2.ft2.coverage.grid.GridCoverage;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Random;

/**
 * Respond to Grid ncss requests
 * 
 */
class GridResponder extends AbstractGridResponder {
  static private final short ESTIMATED_C0MPRESION_RATE = 5;  // Compression rate used to estimate the filesize of netcdf4 compressed files

  ///////////////////////////////////////////////////////////////////////////////
	private GridCoverageDataset gcd;
	private String requestPathInfo;
	private NcssDiskCache ncssDiskCache;

  GridResponder(GridCoverageDataset gcd, String requestPathInfo, NcssDiskCache ncssDiskCache) {
		this.gcd = gcd;
		this.requestPathInfo = requestPathInfo;
		this.ncssDiskCache  = ncssDiskCache;
	}

	/**
	 * Does the actual work.
	 */
	File getResponseFile(HttpServletResponse response, NcssParamsBean params, NetcdfFileWriter.Version version)
			throws NcssException, InvalidRangeException, ParseException, IOException {

		File netcdfResult;
		if (isSpatialSubset(params)) {
			netcdfResult = writeLatLonSubset(params, version);
		} else {
			netcdfResult = writeCoordinatesSubset(params, version);
		}

		return netcdfResult;
	}

	private boolean isSpatialSubset(NcssParamsBean params) throws InvalidBBOXException {
		// LOOK should be in the validator
		boolean spatialSubset = false;
		int contValid = 0;
		if (params.getNorth() != null)
			contValid++;
		if (params.getSouth() != null)
			contValid++;
		if (params.getEast() != null)
			contValid++;
		if (params.getWest() != null)
			contValid++;

		if (contValid == 4) {
			if (params.getNorth() < params.getSouth()) {
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have north > south");
			}
			if (params.getEast() < params.getWest()) {
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have east > west; if crossing 180 meridian, use east boundary > 180");
			}
			spatialSubset = true;

		} else {
			if (contValid > 0)
				throw new InvalidBBOXException("Invalid bbox. All params north, south, east and west must be provided");
			else { // no bbox provided --> is spatialSubsetting
				if (params.getMaxx() == null && params.getMinx() == null
						&& params.getMaxy() == null && params.getMiny() == null)
					spatialSubset = true;
			}
		}
		return spatialSubset;
	}

	private File writeLatLonSubset(NcssParamsBean params, NetcdfFileWriter.Version version) throws RequestTooLargeException,
			OutOfBoundariesException, InvalidRangeException, ParseException,
			IOException, VariableNotContainedInDatasetException,
			InvalidBBOXException, TimeOutOfWindowException {

		LatLonRect maxBB = gcd.getLatLonBoundingBox();
		LatLonRect requestedBB = setBBForRequest(params);

		// LOOK put in the validation code
		boolean hasBB = !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLatitude(), maxBB.getUpperRightPoint().getLatitude())
				|| !ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLatitude(), maxBB.getLowerLeftPoint().getLatitude())
				|| !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLongitude(), maxBB.getUpperRightPoint().getLongitude())
				|| !ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLongitude(), maxBB.getLowerLeftPoint().getLongitude());

		// Don't check this...
		// if (checkBB(maxBB, requestedBB)) {

		Range zRange = null;
		// Request with zRange --> adds a limitation: only variables with the
		// same vertical level???
		if (params.getVertCoord() != null || params.getVertStride() > 1)
			zRange = getZRange(gcd, params.getVertCoord(), params.getVertStride(), params.getVar());

		// List<CalendarDate> wantedDates = getRequestedDates(gcd, params); LOOK old hideous way WTF?
		//CalendarDateRange wantedDateRange = null;
		//if (!wantedDates.isEmpty())
		//	wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get(wantedDates.size() - 1));

		// LOOK should be the calendar of the data!
    return writeGridFile(params.getVar(), hasBB ? requestedBB : null, null, params.getHorizStride(), zRange, params.getCalendarDateRange(null),
            params.getTimeStride(), params.isAddLatLon(), version);
	}

	private File writeCoordinatesSubset(NcssParamsBean params, NetcdfFileWriter.Version version)
			throws OutOfBoundariesException, ParseException, InvalidRangeException, RequestTooLargeException, IOException,
			InvalidBBOXException, TimeOutOfWindowException {

		// Check coordinate params: maxx, maxy, minx, miny
		Double minx = params.getMinx();
		Double maxx = params.getMaxx();
		Double miny = params.getMiny();
		Double maxy = params.getMaxy();

		int contValid = 0;
		if (minx != null)
			contValid++;
		if (maxx != null)
			contValid++;
		if (miny != null)
			contValid++;
		if (maxy != null)
			contValid++;

		if (contValid == 4) {
			if (minx > maxx) {
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have minx < maxx");
			}
			if (miny > maxy) {
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have miny < maxy");
			}

		} else {
			throw new InvalidBBOXException("Invalid bbox. All params minx, maxx. miny, maxy must be provided");
		}

		ProjectionRect rect = new ProjectionRect(minx, miny, maxx, maxy);

		Range zRange = null;
		// Request with zRange --> adds a limitation: only variables with the same vertical level???
		if (params.getVertCoord() != null || params.getVertStride() > 1)
			zRange = getZRange(gcd, params.getVertCoord(), params.getVertStride(), params.getVar());

		/* old hideous way WTF LOOK
		List<CalendarDate> wantedDates = getRequestedDates(gcd, params);
		CalendarDateRange wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get(wantedDates.size() - 1)); */

		// LOOK should be calendar of the data!
    return writeGridFile(params.getVar(), null, rect, params.getHorizStride(), zRange, params.getCalendarDateRange(null), 1, params.isAddLatLon(), version);
	}

  private File writeGridFile(List<String> vars, LatLonRect bbox, ProjectionRect projRect, Integer horizStride,
 			Range zRange, CalendarDateRange dateRange, Integer timeStride, boolean addLatLon, NetcdfFileWriter.Version version)
 			throws RequestTooLargeException, InvalidRangeException, IOException {

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
    if(ncFile == null)
      throw new IllegalStateException("NCSS misconfigured cache = ");
 		String cacheFilename = ncFile.getPath();

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, cacheFilename, null); // default chunking - let user control at some point  LOOK LOOK
    //CFGridWriter2.writeFile(gcd, vars, bbox, projRect, horizStride, zRange, dateRange, timeStride, addLatLon, writer);

 		return new File(cacheFilename);
 	}

	// LOOK in the validation code ??
	private LatLonRect setBBForRequest(NcssParamsBean params) throws InvalidBBOXException {

		if (params.getNorth() == null && params.getSouth() == null
				&& params.getWest() == null && params.getEast() == null)
			return gcd.getLatLonBoundingBox();

		return new LatLonRect(new LatLonPointImpl(params.getSouth(),
				params.getWest()), params.getNorth() - params.getSouth(),
				params.getEast() - params.getWest());
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
						if (vertAxis != null && vertAxis.getNvalues() > 1) {
							int bestIndex = -1;
							double bestDiff = Double.MAX_VALUE;
							for (int i = 0; i < vertAxis.getNvalues(); i++) {
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
							int n = (int) vertAxis.getNvalues();
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
}
