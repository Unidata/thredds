package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.*;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterCSV;
import thredds.server.ncss.view.dsg.station.StationSubsetWriterXML;
import ucar.nc2.constants.FeatureType;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.lang.UnsupportedOperationException;

/**
 * Created by cwardgar on 2014/05/21.
 */
public abstract class DsgSubsetWriterFactory {
    public static DsgSubsetWriter newInstance(OutputStream out, FeatureType featureType, SupportedFormat format)
            throws NcssException, XMLStreamException {
        if (!featureType.isPointFeatureType()) {
            throw new NcssException(String.format("Expected a point feature type, not %s", featureType));
        }

        switch (featureType) {
            case STATION:
                return newStationInstance(out, format);
            default:
                throw new UnsupportedOperationException(
                        String.format("%s feature type is not yet supported.", featureType));
        }
    }

    public static DsgSubsetWriter newStationInstance(OutputStream out, SupportedFormat format)
            throws XMLStreamException, UnsupportedResponseFormatException {
        switch (format) {
            case XML_STREAM:
            case XML_FILE:
                return new StationSubsetWriterXML(out);
            case CSV_STREAM:
            case CSV_FILE:
                return new StationSubsetWriterCSV(out);
            case NETCDF3:
//                w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf3, out);
                return null;
            case NETCDF4:
//                w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf4, out);
                return null;
            case WATERML2:
//                w = new WriterWaterML2(out);
                return null;
            default:
                throw new UnsupportedResponseFormatException("Unknown result type = " + format.getFormatName());
        }
    }

    private DsgSubsetWriterFactory() { }
}
