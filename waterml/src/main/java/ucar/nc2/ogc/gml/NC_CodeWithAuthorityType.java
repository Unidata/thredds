package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.CodeWithAuthorityType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:identifier
 *
 * Created by cwardgar on 2014/02/26.
 */
public class NC_CodeWithAuthorityType extends CodeWithAuthorityType {
    public NC_CodeWithAuthorityType(StationTimeSeriesFeature stationFeat) {
        setValue(stationFeat.getName());
        setCodeSpace("http://unidata.ucar.edu/");
    }
}
