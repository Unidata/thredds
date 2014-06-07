package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimePositionType;
import org.joda.time.DateTime;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NcTimePositionType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/
    //         wml2:MeasurementTVP/wml2:time
    public static TimePositionType initTime(TimePositionType time, PointFeature pointFeat) {
        // TEXT
        time.setStringValue(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition
    public static TimePositionType initBeginPosition(
            TimePositionType beginPosition, StationTimeSeriesFeature stationFeat) throws IOException {
        // TEXT
        stationFeat.calcBounds();
        if (stationFeat.getCalendarDateRange() != null) {
            beginPosition.setStringValue(stationFeat.getCalendarDateRange().getStart().toString());
        }

        return beginPosition;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition
    public static TimePositionType initEndPosition(
            TimePositionType endPosition, StationTimeSeriesFeature stationFeat) throws IOException {
        // TEXT
        stationFeat.calcBounds();
        if (stationFeat.getCalendarDateRange() != null) {
            endPosition.setStringValue(stationFeat.getCalendarDateRange().getEnd().toString());
        }

        return endPosition;
    }

    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:resultTime/gml:TimeInstant/gml:timePosition
    public static TimePositionType initTimePosition(TimePositionType timePosition) {
        DateTime resultTime = MarshallingUtil.fixedResultTime;
        if (resultTime == null) {
            resultTime = new DateTime();  // Initialized to "now".
        }

        // TEXT
        timePosition.setStringValue(resultTime.toString());

        return timePosition;
    }

    private NcTimePositionType() { }
}
