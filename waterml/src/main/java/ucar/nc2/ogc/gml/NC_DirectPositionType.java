package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.DirectPositionType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape/gml:Point/gml:pos
 *
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_DirectPositionType {
    public static DirectPositionType createShapePointPos(StationTimeSeriesFeature stationFeat) {
        DirectPositionType pos = Factories.GML.createDirectPositionType();

        pos.getValues().add(stationFeat.getLatitude());   // gml:pos[0]
        pos.getValues().add(stationFeat.getLongitude());  // gml:pos[1]
        pos.getValues().add(stationFeat.getAltitude());   // gml:pos[2]

        return pos;
    }

    private NC_DirectPositionType() { }
}
