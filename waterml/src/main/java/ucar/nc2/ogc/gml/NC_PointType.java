package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.DirectPositionType;
import net.opengis.gml.v_3_2_1.PointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape/gml:Point
 *
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_PointType {
    public static PointType createShapePoint(StationTimeSeriesFeature stationFeat) {
        PointType point = Factories.GML.createPointType();

        // gml:id
        String id = PointType.class.getSimpleName() + "." + "1";
        point.setId(id);

        // gml:pos
        DirectPositionType pos = NC_DirectPositionType.createPos(stationFeat);
        point.setPos(pos);

        return point;
    }

    private NC_PointType() { }
}
