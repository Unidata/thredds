package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.time.CalendarDateUnit;

/**
 * @author cwardgar
 * @since 2014/08/29
 */
public class SimpleStationPointFeature extends SimplePointFeature implements StationPointFeature {
    private final StationFeature stationFeat;

    public SimpleStationPointFeature(DsgFeatureCollection dsg, StationFeature stationFeat, double obsTime,
            double nomTime, CalendarDateUnit timeUnit, StructureData featureData) {
        super(dsg, stationFeat, obsTime, nomTime, timeUnit, featureData);
        this.stationFeat = stationFeat;
    }

    @Override
    public StationFeature getStation() {
        return stationFeat;
    }
}
