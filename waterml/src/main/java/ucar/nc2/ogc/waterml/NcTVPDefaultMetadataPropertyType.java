package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.DefaultTVPMeasurementMetadataDocument;
import net.opengis.waterml.x20.TVPDefaultMetadataPropertyType;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NcTVPDefaultMetadataPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata
    public static TVPDefaultMetadataPropertyType initDefaultPointMetadata(
            TVPDefaultMetadataPropertyType defaultPointMetadata, VariableSimpleIF dataVar) {
        // wml2:DefaultTVPMetadata
        DefaultTVPMeasurementMetadataDocument defaultTVPMeasurementMetadataDocument =
                DefaultTVPMeasurementMetadataDocument.Factory.newInstance();

        NcTVPMeasurementMetadataType.initDefaultTVPMeasurementMetadata(
                defaultTVPMeasurementMetadataDocument.addNewDefaultTVPMeasurementMetadata(), dataVar);

        defaultPointMetadata.set(defaultTVPMeasurementMetadataDocument);
        return defaultPointMetadata;
    }

    private NcTVPDefaultMetadataPropertyType() { }
}
