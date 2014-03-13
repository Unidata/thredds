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
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_MonitoringPointType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
    public static MonitoringPointType createSfSamplingFeatureType(StationTimeSeriesFeature stationFeat) {
        MonitoringPointType sfSamplingFeatureType = Factories.WATERML.createMonitoringPointType();

        // gml:id
        String id = generateId();
        sfSamplingFeatureType.setId(id);

        // gml:identifier
        CodeWithAuthorityType identifier = NC_CodeWithAuthorityType.createIdentifier(stationFeat);
        sfSamplingFeatureType.setIdentifier(identifier);

        // gml:description
        StringOrRefType description = NC_StringOrRefType.createDescription(stationFeat);
        if (description.getValue() != null && !description.getValue().isEmpty()) {
            sfSamplingFeatureType.setDescription(description);
        }

        // sams:shape
        Shape shape = NC_Shape.createShape(stationFeat);
        sfSamplingFeatureType.setShape(shape);

        return sfSamplingFeatureType;
    }

    private static int numIds = 0;

    private static String generateId() {
        return MonitoringPointType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_MonitoringPointType() { }
}
