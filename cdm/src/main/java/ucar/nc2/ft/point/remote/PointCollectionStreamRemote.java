package ucar.nc2.ft.point.remote;

import java.io.IOException;
import java.io.InputStream;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Creates a {@link ucar.nc2.ft.PointFeatureCollection} by reading point stream data from a CDM Remote endpoint.
 *
 * @author cwardgar
 * @since 2014/10/02
 */
public class PointCollectionStreamRemote extends PointCollectionStreamAbstract implements QueryMaker {
    private final String uri;
    private final QueryMaker queryMaker;
    LatLonRect filter_bb;
    CalendarDateRange filter_date;

    public PointCollectionStreamRemote(String uri, CalendarDateUnit timeUnit, String altUnits, QueryMaker queryMaker) {
        super(uri, timeUnit, altUnits);
        this.uri = uri;
        this.queryMaker = (queryMaker == null) ? this : queryMaker;
    }

    @Override
    public String makeQuery() {
        // LOOK: This is probably broken as of 2015/09/29.
        return PointDatasetRemote.makeQuery(null, filter_bb, filter_date); // default query
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return CdmRemote.sendQuery(null, uri, queryMaker.makeQuery());
    }

    // Must override default subsetting implementation for efficiency.

    @Override
    public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
        return new Subset(this, boundingBox, dateRange);
    }

    private class Subset extends PointCollectionStreamRemote {
        LatLonRect filter_bb;
        CalendarDateRange filter_date;

        Subset(PointCollectionStreamRemote from, LatLonRect filter_bb, CalendarDateRange filter_date)
                throws IOException {
            // Passing null to the queryMaker param causes the default query to be used.
            // The default query will use the boundingBox and dateRange we calculate below.
            super(from.uri, from.getTimeUnit(), from.getAltUnits(), null);

            this.filter_bb = filter_bb;
            this.filter_date = filter_date;
        }
    }
}
