package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.MeasureTVPType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.gml.NcTimePositionType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NcMeasureTVPType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/
    //         wml2:MeasurementTVP
    public static MeasureTVPType initMeasurementTVP(MeasureTVPType measurementTVP, PointFeature pointFeat,
            VariableSimpleIF dataVar) throws IOException {
        // wml2:time
        NcTimePositionType.initTime(measurementTVP.addNewTime(), pointFeat);

        // wml2:value
        NcMeasureType.initValue(measurementTVP.addNewValue(), pointFeat, dataVar);

        return measurementTVP;
    }

    private NcMeasureTVPType() { }
}
