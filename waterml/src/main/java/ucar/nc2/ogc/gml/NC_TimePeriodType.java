package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.TimePeriodType;
import net.opengis.gml.v_3_2_1.TimePosition;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NC_TimePeriodType {
    // om:OM_Observation/om:phenomenonTime/gml:TimePeriod
    public static TimePeriodType createTimePeriod(StationTimeSeriesFeature stationFeat) {
        TimePeriodType timePeriod = Factories.GML.createTimePeriodType();

        // gml:id
        String id = generateId();
        timePeriod.setId(id);

        // gml:beginPoisition
        TimePosition beginPosition = NC_TimePosition.createBeginPosition(stationFeat);
        timePeriod.setBeginPosition(beginPosition);

        // gml:endPosition
        TimePosition endPosition = NC_TimePosition.createEndPosition(stationFeat);
        timePeriod.setEndPosition(endPosition);

        return timePeriod;
    }

    private static int numIds = 0;

    private static String generateId() {
        return TimePeriodType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_TimePeriodType() { }
}
