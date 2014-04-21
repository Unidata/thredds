package ucar.nc2.ogc2.waterml;

import net.opengis.waterml.x20.TVPDefaultMetadataPropertyType;
import net.opengis.waterml.x20.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_TVPDefaultMetadataPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata
    public static TVPDefaultMetadataPropertyType createDefaultPointMetadata(VariableSimpleIF dataVar) {
        TVPDefaultMetadataPropertyType defaultPointMetadata = TVPDefaultMetadataPropertyType.Factory.newInstance();

        // wml2:DefaultTVPMetadata
        TVPMeasurementMetadataType defaultTVPMetadata = NC_TVPMeasurementMetadataType.createDefaultTVPMetadata(dataVar);
        defaultPointMetadata.setDefaultTVPMetadata(defaultTVPMetadata);

        return defaultPointMetadata;
    }

    private NC_TVPDefaultMetadataPropertyType() { }
}
