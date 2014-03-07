package ucar.nc2.ogc.waterml;

import net.opengis.gml.v_3_2_1.TimePosition;
import net.opengis.waterml.v_2_0_1.MeasureTVPType;
import net.opengis.waterml.v_2_0_1.Value;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.gml.NC_TimePosition;

import javax.xml.bind.JAXBElement;
import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasureTVPType {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP
    public static MeasureTVPType createMeasurementTVP(FeatureDatasetPoint fdPoint, PointFeature pointFeat)
            throws IOException {
        MeasureTVPType measurementTVP = Factories.WATERML.createMeasureTVPType();

        // wml2:time
        TimePosition time = NC_TimePosition.createTime(pointFeat);
        measurementTVP.setTime(time);

        // wml2:value
        Value value = NC_Value.createValue(fdPoint, pointFeat);
        JAXBElement<Value> valueElem = Factories.WATERML.createMeasureTVPTypeValue(value);
        measurementTVP.setValue(valueElem);

        return measurementTVP;
    }

    private NC_MeasureTVPType() { }
}
