package thredds.server.ncss.view.dsg.station;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.controller.NcssController;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.ft.point.writer.WriterCFStationCollection;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.DiskCache2;
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

    public StationSubsetWriterNetcdf(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams,
            DiskCache2 diskCache, OutputStream out, NetcdfFileWriter.Version version) throws NcssException, IOException {
        super(fdPoint, ncssParams);

        this.out = out;
        this.version = version;

        this.netcdfResult = diskCache.createUniqueFile("ncssTemp", ".nc");
        List<Attribute> attribs = new ArrayList<>();
        attribs.add(new Attribute(CDM.TITLE, "Extracted data from TDS Feature Collection " + fdPoint.getLocation()));

              // A default value in case we can't find a better one in the dataset.
        // This is the same default that is used in PointDatasetStandard.
        DateUnit timeUnit = DateUnit.factory("seconds since 1970-01-01");

        // If this remains null, it means the dataset has no altitude variable.
        String altUnit = null;

        if (fdPoint.getNetcdfFile() instanceof NetcdfDataset) {
            NetcdfDataset dataset = (NetcdfDataset) fdPoint.getNetcdfFile();

            CoordinateAxis timeAxis = dataset.findCoordinateAxis(AxisType.Time);
            if (timeAxis != null && timeAxis.getUnitsString() != null) {
                timeUnit = DateUnit.factory(timeAxis.getUnitsString());
            }

            CoordinateAxis altAxis = dataset.findCoordinateAxis(AxisType.Height);
            if (altAxis != null) {
                altUnit = altAxis.getUnitsString();
            }
        }

        this.cfWriter = new WriterCFStationCollection( netcdfResult.getAbsolutePath(), attribs, wantedVariables, null,
                timeUnit, altUnit, new CFPointWriterConfig(version) );
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        String fileName = NcssRequestUtils.getFileNameForResponse(datasetPath, version);
        String url = NcssRequestUtils.getTdsContext().getContextPath() +
                NcssController.getServletCachePath() + "/" + fileName;

        if (version == NetcdfFileWriter.Version.netcdf3) {
            httpHeaders.set(ContentType.HEADER, ContentType.netcdf.getContentHeader());
        } else if (version == NetcdfFileWriter.Version.netcdf4 || version == NetcdfFileWriter.Version.netcdf4_classic) {
            httpHeaders.set(ContentType.HEADER, ContentType.netcdf4.getContentHeader());
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
