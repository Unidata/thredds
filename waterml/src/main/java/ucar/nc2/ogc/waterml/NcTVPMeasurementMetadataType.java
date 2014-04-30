package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc.gml.NcReferenceType;
import ucar.nc2.ogc.swe.NcUnitReference;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NcTVPMeasurementMetadataType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata/wml2:DefaultTVPMetadata
    public static TVPMeasurementMetadataType initDefaultTVPMeasurementMetadata(
            TVPMeasurementMetadataType defaultTVPMetadata, VariableSimpleIF dataVar) {
        // wml2:uom
        NcUnitReference.initUom(defaultTVPMetadata.addNewUom(), dataVar);

        // wml2:interpolationType
        NcReferenceType.initInterpolationType(defaultTVPMetadata.addNewInterpolationType());

        return defaultTVPMetadata;
    }

    private NcTVPMeasurementMetadataType() { }
}
