package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.DefaultTVPMeasurementMetadataDocument;
import net.opengis.waterml.x20.TVPDefaultMetadataPropertyType;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NcTVPDefaultMetadataPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/
    //         wml2:defaultPointMetadata
    public static TVPDefaultMetadataPropertyType initDefaultPointMetadata(
            TVPDefaultMetadataPropertyType defaultPointMetadata, VariableSimpleIF dataVar) {
        // wml2:DefaultTVPMeasurementMetadata
        DefaultTVPMeasurementMetadataDocument defaultTVPMeasurementMetadataDoc =
                DefaultTVPMeasurementMetadataDocument.Factory.newInstance();
        NcTVPMeasurementMetadataType.initDefaultTVPMeasurementMetadata(
                defaultTVPMeasurementMetadataDoc.addNewDefaultTVPMeasurementMetadata(), dataVar);
        defaultPointMetadata.set(defaultTVPMeasurementMetadataDoc);

        return defaultPointMetadata;
    }

    private NcTVPDefaultMetadataPropertyType() { }
}
