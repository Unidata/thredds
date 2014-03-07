package ucar.nc2.ogc.waterml;

import net.opengis.gml.v_3_2_1.CodeWithAuthorityType;
import net.opengis.gml.v_3_2_1.StringOrRefType;
import net.opengis.spatialsampling.v_2_0_0.Shape;
import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.gml.NC_CodeWithAuthorityType;
import ucar.nc2.ogc.gml.NC_StringOrRefType;
import ucar.nc2.ogc.spatialsampling.NC_Shape;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
 *     om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType
 *
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_MonitoringPointType {
    public static MonitoringPointType createMonitoringPointType(StationTimeSeriesFeature stationFeat) {
        MonitoringPointType monitoringPointType = Factories.WATERML.createMonitoringPointType();

        // gml:id
        String id = MonitoringPointType.class.getSimpleName() + "." + "1";
        monitoringPointType.setId(id);

        // gml:identifier
        CodeWithAuthorityType identifier = NC_CodeWithAuthorityType.createIdentifier(stationFeat);
        monitoringPointType.setIdentifier(identifier);

        // gml:description
        StringOrRefType description = NC_StringOrRefType.createSamplingFeatureTypeDescription(stationFeat);
        if (description.getValue() != null && !description.getValue().isEmpty()) {
            monitoringPointType.setDescription(description);
        }

        // sams:shape
        Shape shape = NC_Shape.createSpatialSamplingFeatureShape(stationFeat);
        monitoringPointType.setShape(shape);

        return monitoringPointType;
    }

    private NC_MonitoringPointType() { }
}
