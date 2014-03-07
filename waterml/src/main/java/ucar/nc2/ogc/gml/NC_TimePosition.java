package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.TimePosition;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_TimePosition {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:time
    public static TimePosition createTime(PointFeature pointFeat) {
        TimePosition time = Factories.GML.createTimePosition();

        // TEXT
        time.getValues().add(pointFeat.getNominalTimeAsCalendarDate().toString());

        return time;
    }

    private NC_TimePosition() { }
}
