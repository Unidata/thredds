/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.exception.UnsupportedResponseFormatException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.point.PointSubsetWriterCSV;
import thredds.server.ncss.view.dsg.point.PointSubsetWriterGeoCsv;
import thredds.server.ncss.view.dsg.point.PointSubsetWriterNetcdf;
import thredds.server.ncss.view.dsg.point.PointSubsetWriterXML;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterCSV;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterGeoCsv;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterNetcdf;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterWaterML;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterXML;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.DiskCache2;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/05/21.
 */
public abstract class DsgSubsetWriterFactory {
    public static DsgSubsetWriter newInstance(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams,
            DiskCache2 diskCache, OutputStream out, SupportedFormat format)
            throws NcssException, XMLStreamException, IOException {
        FeatureType featureType = fdPoint.getFeatureType();

        if (!featureType.isPointFeatureType()) {
            throw new NcssException(String.format("Expected a point feature type, not %s", featureType));
        }

        switch (featureType) {
            case POINT:
                return newPointInstance(fdPoint, ncssParams, diskCache, out, format);
            case STATION:
                return newStationInstance(fdPoint, ncssParams, diskCache, out, format);
            default:
                throw new UnsupportedOperationException(
                        String.format("%s feature type is not yet supported.", featureType));
        }
    }

    public static DsgSubsetWriter newPointInstance(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams,
            DiskCache2 diskCache, OutputStream out, SupportedFormat format)
            throws XMLStreamException, NcssException, IOException {
        switch (format) {
            case XML_STREAM:
            case XML_FILE:
                return new PointSubsetWriterXML(fdPoint, ncssParams, out);
            case CSV_STREAM:
            case CSV_FILE:
                return new PointSubsetWriterCSV(fdPoint, ncssParams, out);
            case GEOCSV_STREAM:
            case GEOCSV_FILE:
                return new PointSubsetWriterGeoCsv(fdPoint, ncssParams, out);
            case NETCDF3:
                return new PointSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf3);
            case NETCDF4:
                return new PointSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf4_classic);
//            case NETCDF4EXT:
//                return new PointSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf4);
            case WATERML2:
                throw new UnsupportedResponseFormatException(String.format(
                        "%s format not supported for %s feature type.", format, fdPoint.getFeatureType()));
            default:
                throw new UnsupportedResponseFormatException("Unknown result type = " + format.getFormatName());
        }
    }

    public static DsgSubsetWriter newStationInstance(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams,
            DiskCache2 diskCache, OutputStream out, SupportedFormat format)
            throws XMLStreamException, NcssException, IOException {
        switch (format) {
            case XML_STREAM:
            case XML_FILE:
                return new StationSubsetWriterXML(fdPoint, ncssParams, out);
            case CSV_STREAM:
            case CSV_FILE:
                return new StationSubsetWriterCSV(fdPoint, ncssParams, out);
            case GEOCSV_STREAM:
            case GEOCSV_FILE:
                return new StationSubsetWriterGeoCsv(fdPoint, ncssParams, out);
            case NETCDF3:
                return new StationSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf3);
            case NETCDF4:
                return new StationSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf4_classic);
//            case NETCDF4EXT:
//                return new StationSubsetWriterNetcdf(fdPoint, ncssParams, diskCache, out, Version.netcdf4);
            case WATERML2:
                return new StationSubsetWriterWaterML(fdPoint, ncssParams, out);
            default:
                throw new UnsupportedResponseFormatException("Unknown result type = " + format.getFormatName());
        }
    }

    private DsgSubsetWriterFactory() { }
}
