package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.CodeWithAuthorityType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:identifier
 *
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_CodeWithAuthorityType {
    public static CodeWithAuthorityType createSamplingFeatureTypeIdentifier(StationTimeSeriesFeature stationFeat) {
        CodeWithAuthorityType identifier = Factories.GML.createCodeWithAuthorityType();
        identifier.setValue(stationFeat.getName());
        identifier.setCodeSpace("http://unidata.ucar.edu/");
        return identifier;
    }

    private NC_CodeWithAuthorityType() { }
}
