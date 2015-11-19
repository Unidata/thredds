/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.view.dsg.station;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.controller.NcssDiskCache;
import thredds.server.ncss.exception.NcssException;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.ft.point.writer.WriterCFStationCollection;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.Station;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwardgar on 2014/05/29.
 */
public class StationSubsetWriterNetcdf extends AbstractStationSubsetWriter {
    private final OutputStream out;
    private final NetcdfFileWriter.Version version;

    private final File netcdfResult;
    private final WriterCFStationCollection cfWriter;
    private final NcssDiskCache ncssDiskCache;

    public StationSubsetWriterNetcdf(FeatureDatasetPoint fdPoint, SubsetParams ncssParams, NcssDiskCache ncssDiskCache,
                                     OutputStream out, NetcdfFileWriter.Version version) throws NcssException, IOException {
        super(fdPoint, ncssParams);

        this.ncssDiskCache = ncssDiskCache;
        this.out = out;
        this.version = version;

        this.netcdfResult = ncssDiskCache.getDiskCache().createUniqueFile("ncssTemp", ".nc");
        List<Attribute> attribs = new ArrayList<>();
        attribs.add(new Attribute(CDM.TITLE, "Extracted data from TDS Feature Collection " + fdPoint.getLocation()));

        // get the timeUnit and altUnit from the first FeatureCollection
        assert fdPoint.getPointFeatureCollectionList().size() > 0;
        DsgFeatureCollection fc = fdPoint.getPointFeatureCollectionList().get(0);
        CalendarDateUnit timeUnit = fc.getTimeUnit();
        String altUnit = fc.getAltUnits();

        this.cfWriter = new WriterCFStationCollection( netcdfResult.getAbsolutePath(), attribs, wantedVariables,
                timeUnit, altUnit, new CFPointWriterConfig(version) );
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        String fileName = TdsPathUtils.getFileNameForResponse(datasetPath, version);
        String url = ncssDiskCache.getServletCachePath() + fileName;

        if (version == NetcdfFileWriter.Version.netcdf3) {
            httpHeaders.set(ContentType.HEADER, ContentType.netcdf.getContentHeader());
        } else if (version == NetcdfFileWriter.Version.netcdf4 || version == NetcdfFileWriter.Version.netcdf4_classic) {
            httpHeaders.set(ContentType.HEADER, ContentType.netcdf.getContentHeader());
        }

        httpHeaders.set("Content-Location", url);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        return httpHeaders;
    }

    @Override
    protected void writeHeader(StationPointFeature stationPointFeat) throws Exception {
        cfWriter.writeHeader(wantedStations, stationPointFeat);
    }

    @Override
    protected void writeStationPointFeature(StationPointFeature stationPointFeat) throws Exception {
        Station station = stationPointFeat.getStation();
        cfWriter.writeRecord(station, stationPointFeat, stationPointFeat.getFeatureData());
    }

    @Override
    protected void writeFooter() throws Exception {
        cfWriter.finish();
        IO.copyFileB(netcdfResult, out, 60000);  // Copy the file in to the OutputStream.
        out.flush();
    }
}
