package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimePeriodType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionHelper;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.time.CalendarDateRange;

import java.io.IOException;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NcTimePeriodType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime/gml:TimePeriod
    public static TimePeriodType initTimePeriod(
            TimePeriodType timePeriod, StationTimeSeriesFeature stationFeat) throws IOException {
        // @gml:id
        String id = MarshallingUtil.createIdForType(TimePeriodType.class);
        timePeriod.setId(id);

        CollectionInfo info;
        try {
            info = new DsgCollectionHelper(stationFeat).calcBounds();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CalendarDateRange cdr = info.getCalendarDateRange(stationFeat.getTimeUnit());

        // gml:beginPosition
        NcTimePositionType.initBeginPosition(timePeriod.addNewBeginPosition(), cdr.getStart());

        // gml:endPosition
        NcTimePositionType.initEndPosition(timePeriod.addNewEndPosition(), cdr.getEnd());

        return timePeriod;
    }

    private NcTimePeriodType() { }
}
