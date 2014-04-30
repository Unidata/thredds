package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.gml.NcCodeWithAuthorityType;
import ucar.nc2.ogc.gml.NcStringOrRefType;
import ucar.nc2.ogc.spatialsampling.NcShapeType;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NcMonitoringPointType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
    public static MonitoringPointType initSfSamplingFeatureType(
            MonitoringPointType sfSamplingFeatureType, StationTimeSeriesFeature stationFeat) {
        // gml:id
        String id = MarshallingUtil.createIdForType(MonitoringPointType.class);
        sfSamplingFeatureType.setId(id);

        // gml:identifier
        NcCodeWithAuthorityType.initIdentifier(sfSamplingFeatureType.addNewIdentifier(), stationFeat);

        // gml:description
        NcStringOrRefType.initDescription(sfSamplingFeatureType.addNewDescription(), stationFeat);
        if (sfSamplingFeatureType.getDescription().getStringValue() == null ||
                sfSamplingFeatureType.getDescription().getStringValue().isEmpty()) {
            sfSamplingFeatureType.unsetDescription();
        }

        // sam:sampledFeature
        sfSamplingFeatureType.setNilSampledFeature();

        // sams:shape
        NcShapeType.initShape(sfSamplingFeatureType.addNewShape(), stationFeat);

        return sfSamplingFeatureType;
    }

    private NcMonitoringPointType() { }
}
