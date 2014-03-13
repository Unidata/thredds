package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.DirectPositionType;
import net.opengis.gml.v_3_2_1.PointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_PointType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape/gml:Point
    public static PointType createPoint(StationTimeSeriesFeature stationFeat) {
        PointType point = Factories.GML.createPointType();

        // gml:id
        String id = generateId();
        point.setId(id);

        // gml:pos
        DirectPositionType pos = NC_DirectPositionType.createPos(stationFeat);
        point.setPos(pos);

        return point;
    }

    private static int numIds = 0;

    private static String generateId() {
        return PointType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_PointType() { }
}
