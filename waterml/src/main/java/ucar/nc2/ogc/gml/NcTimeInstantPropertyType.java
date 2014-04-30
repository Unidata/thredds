package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimeInstantPropertyType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NcTimeInstantPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:resultTime
    public static TimeInstantPropertyType initResultTime(
            TimeInstantPropertyType resultTime, StationTimeSeriesFeature stationFeat) {
        // gml:TimeInstant
        NcTimeInstantType.initTimeInstant(resultTime.addNewTimeInstant());

        return resultTime;
    }

    private NcTimeInstantPropertyType() { }
}
