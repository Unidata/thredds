package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.PointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NcPointType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint/sams:shape/
    //         gml:Point
    public static PointType initPoint(PointType point, StationTimeSeriesFeature stationFeat) {
        // @gml:id
        String id = MarshallingUtil.createIdForType(PointType.class);
        point.setId(id);

        // gml:pos
        NcDirectPositionType.initPos(point.addNewPos(), stationFeat);

        return point;
    }

    private NcPointType() { }
}
