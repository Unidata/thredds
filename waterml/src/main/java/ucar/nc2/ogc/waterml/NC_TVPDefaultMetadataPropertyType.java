package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.TVPDefaultMetadataPropertyType;
import net.opengis.waterml.v_2_0_1.TVPMeasurementMetadataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc.Factories;

import javax.xml.bind.JAXBElement;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_TVPDefaultMetadataPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata
    public static TVPDefaultMetadataPropertyType createDefaultPointMetadata(VariableSimpleIF dataVar) {
        TVPDefaultMetadataPropertyType defaultPointMetadata = Factories.WATERML.createTVPDefaultMetadataPropertyType();

        // wml2:DefaultTVPMetadata
        TVPMeasurementMetadataType defaultTVPMetadata = NC_TVPMeasurementMetadataType.createDefaultTVPMetadata(dataVar);
        JAXBElement<TVPMeasurementMetadataType> defaultTVPMetadataElem =
                Factories.WATERML.createDefaultTVPMeasurementMetadata(defaultTVPMetadata);
        defaultPointMetadata.setDefaultTVPMetadata(defaultTVPMetadataElem);

        return defaultPointMetadata;
    }

    private NC_TVPDefaultMetadataPropertyType() { }
}
