package thredds.server.ncss.view.dsg.station;

import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.AbstractDsgSubsetWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateRange;
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
    public abstract void writeHeader(List<VariableSimpleIF> wantedVariables) throws Exception;

    public abstract void writePoint(StationPointFeature stationPointFeat, List<VariableSimpleIF> wantedVariables)
            throws Exception;

    public abstract void writeFooter() throws Exception;

    @Override
    public void write(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, ucar.nc2.util.DiskCache2 diskCache)
            throws Exception {
        // Perform variables subset.
        List<VariableSimpleIF> wantedVariables = getWantedVariables(fdPoint, ncssParams);

        writeHeader(wantedVariables);

        List<FeatureCollection> featColList = fdPoint.getPointFeatureCollectionList();
        assert featColList.size() == 1 : "Is there ever a case when this is NOT 1?";
        assert featColList.get(0) instanceof StationTimeSeriesFeatureCollection :
                "This class only deals with StationTimeSeriesFeatureCollections.";

        StationTimeSeriesFeatureCollection stationFeatCol = (StationTimeSeriesFeatureCollection) featColList.get(0);

        // Perform spatial subset.
        List<Station> wantedStations = getStationsInSubset(stationFeatCol, ncssParams);
        StationTimeSeriesFeatureCollection subsettedStationFeatCol = stationFeatCol.subset(wantedStations);

        subsettedStationFeatCol.resetIteration();
        try {
            while (subsettedStationFeatCol.hasNext()) {
                StationTimeSeriesFeature stationFeat = subsettedStationFeatCol.next();

                // Perform temporal subset. We do this even when a time instant is specified, in which case wantedRange
                // represents a sanity check (i.e. "give me the feature closest to the specified time, but it must at
                // least be within an hour").
                CalendarDateRange wantedRange = getWantedRange(ncssParams);
                StationTimeSeriesFeature subsettedStationFeat = stationFeat.subset(wantedRange);

                if (ncssParams.getTime() != null) {
                    DateType wantedDateType = new DateType(ncssParams.getTime(), null, null);  // Parse time string.
                    long wantedTime = wantedDateType.getCalendarDate().getMillis();
                    writePointWithClosestTime(subsettedStationFeat, wantedVariables, wantedTime);
                } else {
                    writeAllPoints(subsettedStationFeat, wantedVariables);
                }
            }
        } finally {
            subsettedStationFeatCol.finish();
        }

        writeFooter();
    }

    protected void writeAllPoints(StationTimeSeriesFeature stationFeat, List<VariableSimpleIF> wantedVariables)
            throws Exception {
        stationFeat.resetIteration();
        try {
            while (stationFeat.hasNext()) {
                PointFeature pointFeat = stationFeat.next();
                assert pointFeat instanceof StationPointFeature :
                        "Expected pointFeat to be a StationPointFeature, not a " + pointFeat.getClass().getSimpleName();
                writePoint((StationPointFeature) pointFeat, wantedVariables);
            }
        } finally {
            stationFeat.finish();
        }
    }

    protected void writePointWithClosestTime(StationTimeSeriesFeature stationFeat,
            List<VariableSimpleIF> wantedVariables, long wantedTime) throws Exception {
        PointFeature pointWithClosestTime = null;
        long smallestDiff = Long.MAX_VALUE;

        stationFeat.resetIteration();
        try {
            while (stationFeat.hasNext()) {
                PointFeature pointFeat = stationFeat.next();
                long obsTime = pointFeat.getObservationTimeAsCalendarDate().getMillis();
                long diff = Math.abs(obsTime - wantedTime);

                if (diff < smallestDiff) {
                    // LOOK: We're caching a PointFeature here. Is this safe?
                    pointWithClosestTime = pointFeat;
                }
            }

            if (pointWithClosestTime != null) {
                assert pointWithClosestTime instanceof StationPointFeature :
                        "Expected pointWithClosestTime to be a StationPointFeature, " +
                        "not a " + pointWithClosestTime.getClass().getSimpleName();
                writePoint((StationPointFeature) pointWithClosestTime, wantedVariables);
            }
        } finally {
            stationFeat.finish();
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
            if (ncssParams.getSouth() == null || ncssParams.getNorth() == null ||
                    ncssParams.getEast() == null || ncssParams.getWest() == null) {
                wantedStations = stationFeatCol.getStations(); //Wants all
            } else {
                LatLonRect llrect = ncssParams.getBB();
                wantedStations = stationFeatCol.getStations(llrect);
            }
        } else if (ncssParams.hasLatLonPoint()) {
            Station closestStation = findClosestStation(
                    stationFeatCol, new LatLonPointImpl(ncssParams.getLatitude(), ncssParams.getLongitude()));
            List<String> stnList = new ArrayList<>();
            stnList.add(closestStation.getName());
            wantedStations = stationFeatCol.getStations(stnList);
        } else { //Want all
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
