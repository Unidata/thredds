package thredds.server.ncss.view.dsg.station;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.AbstractDsgSubsetWriter;
import thredds.server.ncss.view.dsg.FilteredPointFeatureIterator;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateType;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class AbstractStationSubsetWriter extends AbstractDsgSubsetWriter {
    protected final StationTimeSeriesFeatureCollection stationFeatureCollection;
    protected final List<Station> wantedStations;

    public AbstractStationSubsetWriter(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams)
            throws NcssException, IOException {
        super(fdPoint, ncssParams);

        List<FeatureCollection> featColList = fdPoint.getPointFeatureCollectionList();
        assert featColList.size() == 1 : "Is there ever a case when this is NOT 1?";
        assert featColList.get(0) instanceof StationTimeSeriesFeatureCollection :
                "This class only deals with StationTimeSeriesFeatureCollections.";

        this.stationFeatureCollection = (StationTimeSeriesFeatureCollection) featColList.get(0);
        this.wantedStations = getStationsInSubset(stationFeatureCollection, ncssParams);
    }

    protected abstract void writeHeader() throws Exception;

    protected abstract void writeStationPointFeature(StationPointFeature stationPointFeat) throws Exception;

    protected abstract void writeFooter() throws Exception;

    @Override
    public void write() throws Exception {
        writeHeader();

        // Perform spatial subset.
        StationTimeSeriesFeatureCollection subsettedStationFeatCol = stationFeatureCollection.subset(wantedStations);

        subsettedStationFeatCol.resetIteration();
        try {
            while (subsettedStationFeatCol.hasNext()) {
                StationTimeSeriesFeature stationFeat = subsettedStationFeatCol.next();

                // Perform temporal subset. We do this even when a time instant is specified, in which case wantedRange
                // represents a sanity check (i.e. "give me the feature closest to the specified time, but it must at
                // least be within an hour").
                StationTimeSeriesFeature subsettedStationFeat = stationFeat.subset(wantedRange);

                if (ncssParams.getTime() != null) {
                    DateType wantedDateType = new DateType(ncssParams.getTime(), null, null);  // Parse time string.
                    CalendarDate wantedTime = wantedDateType.getCalendarDate();
                    subsettedStationFeat = new ClosestTimeStationFeatureSubset(
                            (StationFeatureImpl) subsettedStationFeat, wantedTime);
                }

                writeStationTimeSeriesFeature(subsettedStationFeat);
            }
        } finally {
            subsettedStationFeatCol.finish();
        }

        writeFooter();
    }

    protected void writeStationTimeSeriesFeature(StationTimeSeriesFeature stationFeat)
            throws Exception {
        stationFeat.resetIteration();
        try {
            while (stationFeat.hasNext()) {
                PointFeature pointFeat = stationFeat.next();
                assert pointFeat instanceof StationPointFeature :
                        "Expected pointFeat to be a StationPointFeature, not a " + pointFeat.getClass().getSimpleName();
                writeStationPointFeature((StationPointFeature) pointFeat);
            }
        } finally {
            stationFeat.finish();
        }
    }

    protected static class ClosestTimeStationFeatureSubset extends StationFeatureImpl {
        private final StationTimeSeriesFeature stationFeat;
        private CalendarDate closestTime;

        protected ClosestTimeStationFeatureSubset(
                StationFeatureImpl stationFeat, CalendarDate wantedTime) throws IOException {
            super(stationFeat, stationFeat.getTimeUnit(), -1);
            this.stationFeat = stationFeat;
            this.dateRange = stationFeat.getCalendarDateRange();

            long smallestDiff = Long.MAX_VALUE;

            stationFeat.resetIteration();
            try {
                while (stationFeat.hasNext()) {
                    PointFeature pointFeat = stationFeat.next();
                    CalendarDate obsTime = pointFeat.getObservationTimeAsCalendarDate();
                    long diff = Math.abs(obsTime.getMillis() - wantedTime.getMillis());

                    if (diff < smallestDiff) {
                        closestTime = obsTime;
                    }
                }
            } finally {
                stationFeat.finish();
            }
        }

        // Filter out PointFeatures that don't have the wantedTime.
        protected static class TimeFilter implements PointFeatureIterator.Filter {
            private final CalendarDate wantedTime;

            protected TimeFilter(CalendarDate wantedTime) {
                this.wantedTime = wantedTime;
            }

            @Override
            public boolean filter(PointFeature pointFeature) {
                return pointFeature.getObservationTimeAsCalendarDate().equals(wantedTime);
            }
        }

        @Override
        public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
            if (closestTime == null) {
                return stationFeat.getPointFeatureIterator(bufferSize);
            } else {
                return new FilteredPointFeatureIterator(
                        stationFeat.getPointFeatureIterator(bufferSize), new TimeFilter(closestTime));
            }
        }
    }


    // LOOK could do better : "all", and maybe HashSet<Name>
    public static List<Station> getStationsInSubset(
            StationTimeSeriesFeatureCollection stationFeatCol, NcssParamsBean ncssParams) throws IOException {
        List<Station> wantedStations;

        // verify SpatialSelection has some stations
        if (ncssParams.hasStations()) {
            List<String> stnNames = ncssParams.getStns();

            if (stnNames.get(0).equals("all")) {
                wantedStations = stationFeatCol.getStations();
            } else {
                wantedStations = stationFeatCol.getStations(stnNames);
            }
        } else if (ncssParams.hasLatLonBB()) {
            LatLonRect llrect = ncssParams.getBoundingBox();
            wantedStations = stationFeatCol.getStations(llrect);
        } else if (ncssParams.hasLatLonPoint()) {
            Station closestStation = findClosestStation(
                    stationFeatCol, new LatLonPointImpl(ncssParams.getLatitude(), ncssParams.getLongitude()));
            List<String> stnList = new ArrayList<>();
            stnList.add(closestStation.getName());
            wantedStations = stationFeatCol.getStations(stnList);
        } else { // Want all.
            wantedStations = stationFeatCol.getStations();
        }

        return wantedStations;
    }

    /*
     * Find the station closest to the specified point.
     * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
     *
     * @param lat latitude value
     * @param lon longitude value
     * @return name of station closest to the specified point
     * @throws IOException if read error
     */
    public static Station findClosestStation(StationTimeSeriesFeatureCollection stationFeatCol, LatLonPoint pt)
            throws IOException {
        double lat = pt.getLatitude();
        double lon = pt.getLongitude();
        double cos = Math.cos(Math.toRadians(lat));
        List<Station> stations = stationFeatCol.getStations();
        Station min_station = stations.get(0);
        double min_dist = Double.MAX_VALUE;

        for (Station s : stations) {
            double lat1 = s.getLatitude();
            double lon1 = LatLonPointImpl.lonNormal(s.getLongitude(), lon);
            double dy = Math.toRadians(lat - lat1);
            double dx = cos * Math.toRadians(lon - lon1);
            double dist = dy * dy + dx * dx;
            if (dist < min_dist) {
                min_dist = dist;
                min_station = s;
            }
        }
        return min_station;
    }
}
