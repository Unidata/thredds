package ucar.nc2.ogc.waterml;

import net.opengis.swe.v_2_0_0.UnitReference;
import net.opengis.waterml.v_2_0_1.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.swe.NC_UnitReference;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_TVPMeasurementMetadataType {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata/wml2:DefaultTVPMetadata
    public static TVPMeasurementMetadataType createDefaultTVPMetadata(VariableSimpleIF dataVar) {
        TVPMeasurementMetadataType defaultTVPMetadata = Factories.WATERML.createTVPMeasurementMetadataType();

        // wml2:uom
        UnitReference uom = NC_UnitReference.createUom(dataVar);
        defaultTVPMetadata.setUom(uom);

        return defaultTVPMetadata;
    }

    private NC_TVPMeasurementMetadataType() { }
}
