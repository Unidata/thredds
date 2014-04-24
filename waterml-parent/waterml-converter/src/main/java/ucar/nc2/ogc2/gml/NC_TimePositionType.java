package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.TimePositionType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_TimePositionType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:time
    public static TimePositionType createTime(PointFeature pointFeat) {
        TimePositionType time = TimePositionType.Factory.newInstance();

        // TEXT
        time.setStringValue(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    public static TimePositionType initTime(TimePositionType time, PointFeature pointFeat) {
        // TEXT
        time.setStringValue(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosiition
    public static TimePositionType createBeginPosition(StationTimeSeriesFeature stationFeat) {
        TimePositionType beginPosition = TimePositionType.Factory.newInstance();

        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            beginPosition.setStringValue(stationFeat.getCalendarDateRange().getStart().toString());
        }

        return beginPosition;
    }

    public static TimePositionType initBeginPosition(TimePositionType beginPosition, StationTimeSeriesFeature stationFeat) {
        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            beginPosition.setStringValue(stationFeat.getCalendarDateRange().getStart().toString());
        }

        return beginPosition;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosiition
    public static TimePositionType createEndPosition(StationTimeSeriesFeature stationFeat) {
        TimePositionType endPosition = TimePositionType.Factory.newInstance();

        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            endPosition.setStringValue(stationFeat.getCalendarDateRange().getEnd().toString());
        }

        return endPosition;
    }

    public static TimePositionType initEndPosition(TimePositionType endPosition, StationTimeSeriesFeature stationFeat) {
        // TEXT
        if (stationFeat.getCalendarDateRange() != null) {
            endPosition.setStringValue(stationFeat.getCalendarDateRange().getEnd().toString());
        }

        return endPosition;
    }

    private NC_TimePositionType() { }
}
