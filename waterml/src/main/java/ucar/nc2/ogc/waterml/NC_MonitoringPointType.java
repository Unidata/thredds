package ucar.nc2.ogc.waterml;

import net.opengis.gml.v_3_2_1.CodeWithAuthorityType;
import net.opengis.gml.v_3_2_1.StringOrRefType;
import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.gml.NC_CodeWithAuthorityType;
import ucar.nc2.ogc.gml.NC_StringOrRefType;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
 *     om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType
 *
 * Created by cwardgar on 2014/02/26.
 */
public class NC_MonitoringPointType extends MonitoringPointType {
    public NC_MonitoringPointType(StationTimeSeriesFeature stationFeat) {
        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:id
        String id = super.getClass().getSimpleName() + "." + "1";
        setId(id);

        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:identifier
        CodeWithAuthorityType identifier = new NC_CodeWithAuthorityType(stationFeat);
        setIdentifier(identifier);

        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:description
        StringOrRefType description = new NC_StringOrRefType(stationFeat);
        if (description.getValue() != null) {
            setDescription(description);
        }
    }
}
