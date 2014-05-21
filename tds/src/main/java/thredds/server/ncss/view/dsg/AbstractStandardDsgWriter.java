package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeatureCollection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class AbstractStandardDsgWriter extends AbstractDsgWriter {
    protected final PrintWriter outWriter;

    protected AbstractStandardDsgWriter(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams,
                                        ucar.nc2.util.DiskCache2 diskCache, OutputStream outStream)
            throws IOException, NcssException {
        super(fdPoint, ncssParams, diskCache);
        this.outWriter = new PrintWriter(outStream);
    }

    public abstract void writeHeader() throws IOException;

    public abstract void writePoint() throws IOException;

    public abstract void writeFooter() throws IOException;

    protected abstract PointFeatureCollection getPointFeatureCollection();

    @Override public void write() throws ParseException, IOException, NcssException {
        /*
        Action act = writer.getAction();
        writer.header();

        if (ncssParams.getTime() != null) {
            scanForClosestTime(pfc, new DateType(ncssParams.getTime(), null, null), null, act, counter);

        } else {
            scan(pfc, wantRange, null, act, counter);
        }

        writer.trailer();
        */

        writeHeader();
    }
}
