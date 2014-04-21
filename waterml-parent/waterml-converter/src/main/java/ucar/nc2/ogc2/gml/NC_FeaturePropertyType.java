package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.FeaturePropertyType;
import net.opengis.waterml.x20.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc2.waterml.NC_MonitoringPointType;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_FeaturePropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest
    public static FeaturePropertyType createFeatureOfInterest(StationTimeSeriesFeature stationFeat) {
        FeaturePropertyType featureOfInterest = FeaturePropertyType.Factory.newInstance();

        // sam:SF_SamplingFeatureType
        MonitoringPointType sfSamplingFeatureType = NC_MonitoringPointType.createSfSamplingFeatureType(stationFeat);
        featureOfInterest.setAbstractFeature(sfSamplingFeatureType);

        return featureOfInterest;
    }

    private NC_FeaturePropertyType() { }
}
