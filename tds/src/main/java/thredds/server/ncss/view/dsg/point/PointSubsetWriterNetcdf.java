/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.view.dsg.point;

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
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwardgar on 2014/06/04.
 */
public class PointSubsetWriterNetcdf extends AbstractPointSubsetWriter {
    private final NcssDiskCache ncssDiskCache;
    private final OutputStream out;
    private final NetcdfFileWriter.Version version;

    private final File netcdfResult;
    private final WriterCFPointCollection cfWriter;

    public PointSubsetWriterNetcdf(FeatureDatasetPoint fdPoint, SubsetParams ncssParams, NcssDiskCache ncssDiskCache,
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

        this.cfWriter = new WriterCFPointCollection(netcdfResult.getAbsolutePath(), attribs, wantedVariables,
                timeUnit, altUnit, new CFPointWriterConfig(version));
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
    public void writeHeader(PointFeature pf) throws IOException {
        cfWriter.writeHeader(pf);
    }

    @Override
    public void writePoint(PointFeature pointFeat) throws Exception {
        cfWriter.writeRecord(pointFeat, pointFeat.getDataAll());
    }

    @Override
    public void writeFooter() throws IOException {
        cfWriter.finish();
        IO.copyFileB(netcdfResult, out, 60000);  // Copy the file in to the OutputStream.
        out.flush();
    }
}
