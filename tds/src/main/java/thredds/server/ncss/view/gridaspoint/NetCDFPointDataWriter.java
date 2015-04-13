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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import thredds.server.ncss.controller.GridDatasetResponder;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.view.gridaspoint.netcdf.CFPointWriterWrapper;
import thredds.server.ncss.view.gridaspoint.netcdf.CFPointWriterWrapperFactory;
import thredds.util.ContentType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.LatLonPoint;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class NetCDFPointDataWriter implements PointDataWriter {
    static private Logger log = LoggerFactory.getLogger(NetCDFPointDataWriter.class);

    public static NetCDFPointDataWriter factory(
            NetcdfFileWriter.Version version, OutputStream outputStream, DiskCache2 diskCache) {
        return new NetCDFPointDataWriter(version, outputStream, diskCache);
    }

    ///////////////////////////////////////////////////////////////////

    private OutputStream outputStream;
    private DiskCache2 diskCache;
    private File netcdfResult;
    //private boolean isProfile = false;
    private NetcdfFileWriter.Version version;
    private CF.FeatureType featureType;
    private CFPointWriterWrapper pointWriterWrapper;
    private HttpHeaders httpHeaders = new HttpHeaders();
    //private List<VariableSimpleIF> wantedVars;

    private NetCDFPointDataWriter(NetcdfFileWriter.Version version, OutputStream outputStream, DiskCache2 diskCache) {
        this.outputStream = outputStream;
        this.version = version;
        this.diskCache = diskCache;
        netcdfResult = diskCache.createUniqueFile("ncss", ".nc");
    }

    //public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates,
    // DateUnit dateUnit,LatLonPoint point, Double vertCoord) {
    public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates,
            List<Attribute> timeDimAtts, LatLonPoint point, Double vertCoord) {

        boolean headerDone = false;
        if (groupedVars.size() > 1 && !wDates.isEmpty()) { //Variables with different vertical levels
            //featureType = CF.FeatureType.profile;
            featureType = CF.FeatureType.timeSeriesProfile;

        } else {
            List<String> keys = new ArrayList<>(groupedVars.keySet());
            List<String> varsForRequest = groupedVars.get(keys.get(0));
            CoordinateAxis1D zAxis = gridDataset.findGridDatatype(varsForRequest.get(0)).getCoordinateSystem()
                    .getVerticalAxis();

            if (wDates.isEmpty()) {// Point feature with no time axis!!!
                featureType = CF.FeatureType.point;

            } else if (zAxis == null) {//Station
                featureType = CF.FeatureType.timeSeries;
            } else {//Time series profile with one variable
                featureType = CF.FeatureType.timeSeriesProfile;
            }
        }

        try {
            List<Attribute> atts = new ArrayList<>();
            atts.add(new Attribute(CDM.TITLE, "Extract Points data from Grid file " + gridDataset.getLocation()));
            pointWriterWrapper = CFPointWriterWrapperFactory.getWriterForFeatureType(
                    version, featureType, netcdfResult.getAbsolutePath(), atts);
            headerDone = pointWriterWrapper.header(groupedVars, gridDataset, wDates, timeDimAtts, point, vertCoord);
        } catch (IOException ioe) {
            log.error("Error writing header", ioe);
        }

        return headerDone;
    }


    public boolean write(Map<String, List<String>> groupedVars, GridDataset gds, List<CalendarDate> wDates,
            LatLonPoint point, Double vertCoord) throws InvalidRangeException {
        if (wDates.isEmpty()) {
            return write(groupedVars, gds, CalendarDate.of(new Date()), point, vertCoord);
        }

        //loop over wDates
        CalendarDate date;
        Iterator<CalendarDate> it = wDates.iterator();
        boolean pointRead = true;

        while (pointRead && it.hasNext()) {
            date = it.next();
            pointRead = write(groupedVars, gds, date, point, vertCoord);

        }

        return pointRead;
    }

    private boolean write(Map<String, List<String>> groupedVars, GridDataset gridDataset, CalendarDate date,
            LatLonPoint point, Double targetLevel) throws InvalidRangeException {
        return pointWriterWrapper.write(groupedVars, gridDataset, date, point, targetLevel);
    }

    @Override
    public boolean trailer() {
        boolean allDone = false;
        pointWriterWrapper.trailer();

        try {
            IO.copyFileB(netcdfResult, outputStream, 60000);
            allDone = true;
        } catch (IOException ioe) {
            log.error("Error copying result to the output stream", ioe);
        }

        return allDone;
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        return httpHeaders;
    }

    @Override
    public void setHTTPHeaders(GridDataset gridDataset, String pathInfo, boolean isStream) {
        //Set the response headers...
        String fileName = NcssRequestUtils.getFileNameForResponse(pathInfo, version);
        String url = GridDatasetResponder.buildCacheUrl(netcdfResult.getName());
        String contentType = SupportedFormat.NETCDF3.getResponseContentType();

        if (version == NetcdfFileWriter.Version.netcdf4) {
            contentType = SupportedFormat.NETCDF4.getResponseContentType();
        }

        httpHeaders.set(ContentType.HEADER, contentType);
        httpHeaders.set("Content-Location", url);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    }
}
