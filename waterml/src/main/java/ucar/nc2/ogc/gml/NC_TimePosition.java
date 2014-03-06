package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.TimePosition;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.Factories;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:time
 *
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_TimePosition {
    public static TimePosition createTime(PointFeature pointFeat) {
        TimePosition time = Factories.GML.createTimePosition();

        time.getValues().add(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    private NC_TimePosition() { }
}
