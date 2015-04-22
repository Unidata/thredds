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
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint
    public static MonitoringPointType initMonitoringPointType(
            MonitoringPointType monitoringPoint, StationTimeSeriesFeature stationFeat) {
        // @gml:id
        String id = MarshallingUtil.createIdForType(MonitoringPointType.class);
        monitoringPoint.setId(id);

        // gml:identifier
        NcCodeWithAuthorityType.initIdentifier(monitoringPoint.addNewIdentifier(), stationFeat);

        // gml:description
        NcStringOrRefType.initDescription(monitoringPoint.addNewDescription(), stationFeat);
        if (monitoringPoint.getDescription().getStringValue() == null ||
                monitoringPoint.getDescription().getStringValue().isEmpty()) {
            monitoringPoint.unsetDescription();
        }

        // sam:sampledFeature
        monitoringPoint.addNewSampledFeature();
        monitoringPoint.setNilSampledFeatureArray(0);  // Set the "sam:sampledFeature" we just added to nil.

        // sams:shape
        NcShapeType.initShape(monitoringPoint.addNewShape(), stationFeat);

        return monitoringPoint;
    }

    private NcMonitoringPointType() { }
}
