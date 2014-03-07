package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.DirectPositionType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NC_DirectPositionType {
    // om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeatureType/sams:shape/gml:Point/gml:pos
    public static DirectPositionType createPos(StationTimeSeriesFeature stationFeat) {
        DirectPositionType pos = Factories.GML.createDirectPositionType();

        // TEXT
        pos.getValues().add(stationFeat.getLatitude());
        pos.getValues().add(stationFeat.getLongitude());
        pos.getValues().add(stationFeat.getAltitude());

        return pos;
    }

    private NC_DirectPositionType() { }
}
