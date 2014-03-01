package ucar.nc2.ogc.om;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.gml.NC_FeaturePropertyType;

import java.io.IOException;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation
 *
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_OMObservationType {
    private static final net.opengis.om.v_2_0_0.ObjectFactory omObjectFactory =
            new net.opengis.om.v_2_0_0.ObjectFactory();

    public static OMObservationType createObservationType(FeatureDatasetPoint fdPoint) throws IOException {
        OMObservationType obsType = omObjectFactory.createOMObservationType();

        // gml:id
        String id = OMObservationType.class.getSimpleName() + "." + "1";
        obsType.setId(id);

        StationTimeSeriesFeature stationFeat = PointUtil.getSingleStationFeatureFromDataset(fdPoint);

        // om:featureOfInterest
        FeaturePropertyType featureOfInterest = NC_FeaturePropertyType.createFeatureOfInterest(stationFeat);
        obsType.setFeatureOfInterest(featureOfInterest);

        return obsType;
    }

    private NC_OMObservationType() { }
}
