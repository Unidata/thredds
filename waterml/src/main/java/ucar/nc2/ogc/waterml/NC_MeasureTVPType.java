package ucar.nc2.ogc.waterml;

import net.opengis.gml.v_3_2_1.TimePosition;
import net.opengis.waterml.v_2_0_1.MeasureTVPType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.gml.NC_TimePosition;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP
 *
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasureTVPType {
    public static MeasureTVPType createMeasurementTVP(PointFeature pointFeat) {
        MeasureTVPType measurementTVP = Factories.WATERML.createMeasureTVPType();

        // wml2:time
        TimePosition time = NC_TimePosition.createTime(pointFeat);
        measurementTVP.setTime(time);

        return measurementTVP;
    }

    private NC_MeasureTVPType() { }
}
