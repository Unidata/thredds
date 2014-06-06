package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimePeriodType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;

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

        // gml:beginPosition
        NcTimePositionType.initBeginPosition(timePeriod.addNewBeginPosition(), stationFeat);

        // gml:endPosition
        NcTimePositionType.initEndPosition(timePeriod.addNewEndPosition(), stationFeat);

        return timePeriod;
    }

    private NcTimePeriodType() { }
}
