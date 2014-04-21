package ucar.nc2.ogc2.waterml;

import net.opengis.swe.x20.UnitReference;
import net.opengis.waterml.x20.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc2.swe.NC_UnitReference;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_TVPMeasurementMetadataType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata/wml2:DefaultTVPMetadata
    public static TVPMeasurementMetadataType createDefaultTVPMetadata(VariableSimpleIF dataVar) {
        TVPMeasurementMetadataType defaultTVPMetadata = TVPMeasurementMetadataType.Factory.newInstance();

        // wml2:uom
        UnitReference uom = NC_UnitReference.createUom(dataVar);
        defaultTVPMetadata.setUom(uom);

        return defaultTVPMetadata;
    }

    private NC_TVPMeasurementMetadataType() { }
}
