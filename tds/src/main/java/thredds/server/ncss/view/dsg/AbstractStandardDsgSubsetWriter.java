package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.ma2.StructureData;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class AbstractStandardDsgSubsetWriter extends AbstractDsgSubsetWriter {
    public abstract void writeHeader() throws IOException;

    public abstract void writePoint(PointFeature pf, StructureData sdata) throws IOException;

    public abstract void writeFooter() throws IOException;

    protected abstract PointFeatureCollection getPointFeatureCollection();

    @Override
    public void write(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, ucar.nc2.util.DiskCache2 diskCache)
            throws ParseException, IOException, NcssException {
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



        writeFooter();
    }
}
