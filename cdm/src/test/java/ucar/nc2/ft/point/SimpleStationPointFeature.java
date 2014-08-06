package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.nc2.units.DateUnit;

/**
 * @author cwardgar
 * @since 2014/08/29
 */
public class SimpleStationPointFeature extends SimplePointFeature implements StationPointFeature {
    private final StationFeature stationFeat;

    public SimpleStationPointFeature(
            StationFeature stationFeat, double obsTime, double nomTime, DateUnit timeUnit, StructureData featureData) {
        super(stationFeat, obsTime, nomTime, timeUnit, featureData);
        this.stationFeat = stationFeat;
    }

    @Override
    public StationFeature getStation() {
        return stationFeat;
    }
}
