package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.FeaturePropertyType;
import net.opengis.waterml.x20.MonitoringPointDocument;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.waterml.NC_MonitoringPointType;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_FeaturePropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest
    public static FeaturePropertyType initFeatureOfInterest(
            FeaturePropertyType featureOfInterest, StationTimeSeriesFeature stationFeat) {
        // sam:SF_SamplingFeatureType
        MonitoringPointDocument monitoringPointDoc = MonitoringPointDocument.Factory.newInstance();

        NC_MonitoringPointType.initSfSamplingFeatureType(monitoringPointDoc.addNewMonitoringPoint(), stationFeat);

        featureOfInterest.set(monitoringPointDoc);
        return featureOfInterest;
    }

    private NC_FeaturePropertyType() { }
}
