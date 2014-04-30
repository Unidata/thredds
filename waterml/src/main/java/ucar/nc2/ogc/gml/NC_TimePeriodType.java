package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimePeriodType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NC_TimePeriodType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod
    public static TimePeriodType initTimePeriod(TimePeriodType timePeriod, StationTimeSeriesFeature stationFeat) {
        // gml:id
        String id = generateId();
        timePeriod.setId(id);

        // gml:beginPosition
        NC_TimePositionType.initBeginPosition(timePeriod.addNewBeginPosition(), stationFeat);

        // gml:endPosition
        NC_TimePositionType.initEndPosition(timePeriod.addNewEndPosition(), stationFeat);

        return timePeriod;
    }

    private static int numIds = 0;

    private static String generateId() {
        return TimePeriodType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_TimePeriodType() { }
}
