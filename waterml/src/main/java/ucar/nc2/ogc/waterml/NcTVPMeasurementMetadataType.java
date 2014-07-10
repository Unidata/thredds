package ucar.nc2.ogc.waterml;

import net.opengis.swe.x20.UnitReference;
import net.opengis.waterml.x20.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc.gml.NcReferenceType;
import ucar.nc2.ogc.swe.NcUnitReference;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NcTVPMeasurementMetadataType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/
    //         wml2:defaultPointMetadata/wml2:DefaultTVPMeasurementMetadata
    public static TVPMeasurementMetadataType initDefaultTVPMeasurementMetadata(
            TVPMeasurementMetadataType defaultTVPMeasurementMetadata, VariableSimpleIF dataVar) {
        // wml2:uom
        UnitReference uom = NcUnitReference.initUom(defaultTVPMeasurementMetadata.addNewUom(), dataVar);
        if (uom == null) {
            defaultTVPMeasurementMetadata.unsetUom();
        }

        // wml2:interpolationType
        NcReferenceType.initInterpolationType(defaultTVPMeasurementMetadata.addNewInterpolationType());

        return defaultTVPMeasurementMetadata;
    }

    private NcTVPMeasurementMetadataType() { }
}
