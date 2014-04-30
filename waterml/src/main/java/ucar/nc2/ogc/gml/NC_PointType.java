package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.PointType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_PointType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape/gml:Point
    public static PointType initPoint(PointType point, StationTimeSeriesFeature stationFeat) {
        // gml:id
        String id = generateId();
        point.setId(id);

        // gml:pos
        NC_DirectPositionType.initPos(point.addNewPos(), stationFeat);

        return point;
    }

    private static int numIds = 0;

    private static String generateId() {
        return PointType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_PointType() { }
}
