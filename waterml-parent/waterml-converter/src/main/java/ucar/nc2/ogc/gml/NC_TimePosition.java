package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.TimePosition;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_TimePosition {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:time
    public static TimePosition createTime(PointFeature pointFeat) {
        TimePosition time = Factories.GML.createTimePosition();

        // TEXT
        time.getValues().add(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosiition
    public static TimePosition createBeginPosition(StationTimeSeriesFeature stationFeat) {
        TimePosition beginPosition = Factories.GML.createTimePosition();

        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            beginPosition.getValues().add(stationFeat.getCalendarDateRange().getStart().toString());
        }

        return beginPosition;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosiition
    public static TimePosition createEndPosition(StationTimeSeriesFeature stationFeat) {
        TimePosition endPosition = Factories.GML.createTimePosition();

        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            endPosition.getValues().add(stationFeat.getCalendarDateRange().getEnd().toString());
        }

        return endPosition;
    }

    private NC_TimePosition() { }
}
