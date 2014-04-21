package ucar.nc2.ogc2.waterml;

import net.opengis.waterml.x20.MeasureType;
import net.opengis.gml.x32.TimePositionType;
import net.opengis.waterml.x20.MeasureTVPType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc2.gml.NC_TimePositionType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasureTVPType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP
    public static MeasureTVPType createMeasurementTVP(PointFeature pointFeat, VariableSimpleIF dataVar)
            throws IOException {
        MeasureTVPType measurementTVP = MeasureTVPType.Factory.newInstance();

        // wml2:time
        TimePositionType time = NC_TimePositionType.createTime(pointFeat);
        measurementTVP.setTime(time);

        // wml2:value
        MeasureType value = NC_MeasureType.createValue(pointFeat, dataVar);
        measurementTVP.setValue(value);

        return measurementTVP;
    }

    private NC_MeasureTVPType() { }
}
