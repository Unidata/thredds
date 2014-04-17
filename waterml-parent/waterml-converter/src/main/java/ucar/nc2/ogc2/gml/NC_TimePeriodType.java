package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.TimePeriodType;
import net.opengis.gml.x32.TimePositionType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NC_TimePeriodType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod
    public static TimePeriodType createTimePeriod(StationTimeSeriesFeature stationFeat) {
        TimePeriodType timePeriod = TimePeriodType.Factory.newInstance();

        // gml:id
        String id = generateId();
        timePeriod.setId(id);

        // gml:beginPosition
        TimePositionType beginPosition = NC_TimePositionType.createBeginPosition(stationFeat);
        timePeriod.setBeginPosition(beginPosition);

        // gml:endPosition
        TimePositionType endPosition = NC_TimePositionType.createEndPosition(stationFeat);
        timePeriod.setEndPosition(endPosition);

        return timePeriod;
    }

    private static int numIds = 0;

    private static String generateId() {
        return TimePeriodType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_TimePeriodType() { }
}
