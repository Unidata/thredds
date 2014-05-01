package ucar.nc2.ogc.om;

import net.opengis.gml.x32.TimePeriodDocument;
import net.opengis.om.x20.TimeObjectPropertyType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.gml.NcTimePeriodType;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NcTimeObjectPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:phenomenonTime
    public static TimeObjectPropertyType initPhenomenonTime(
            TimeObjectPropertyType phenomenonTime, StationTimeSeriesFeature stationFeat) {
        // gml:TimePeriod
        TimePeriodDocument timePeriodDoc = TimePeriodDocument.Factory.newInstance();
        NcTimePeriodType.initTimePeriod(timePeriodDoc.addNewTimePeriod(), stationFeat);
        phenomenonTime.set(timePeriodDoc);

        return phenomenonTime;
    }

    private NcTimeObjectPropertyType() { }
}
