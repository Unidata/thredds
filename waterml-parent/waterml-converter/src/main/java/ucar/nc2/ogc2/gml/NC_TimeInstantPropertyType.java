package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.TimeInstantPropertyType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NC_TimeInstantPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:resultTime
    public static TimeInstantPropertyType createResultTime(StationTimeSeriesFeature stationFeat) {
        TimeInstantPropertyType resultTime = TimeInstantPropertyType.Factory.newInstance();

        // StationTimeSeriesFeature has no comparable property.

        return resultTime;
    }

    private NC_TimeInstantPropertyType() { }
}
