package ucar.nc2.ogc.om;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ogc.gml.NC_FeaturePropertyType;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation
 *
 * Created by cwardgar on 2014/02/26.
 */
public class NC_OMObservationType extends OMObservationType {
    public NC_OMObservationType(FeatureDatasetPoint fdPoint) throws IOException {
        // om:OM_Observation/gml:id
        String id = super.getClass().getSimpleName() + "." + "1";
        setId(id);

        StationTimeSeriesFeature stationFeat = getSingleStationFeatureFromDataset(fdPoint);

        // om:OM_Observation/om:featureOfInterest
        FeaturePropertyType featureOfInterest = new NC_FeaturePropertyType(stationFeat);
        setFeatureOfInterest(featureOfInterest);
    }

    public static StationTimeSeriesFeature getSingleStationFeatureFromDataset(FeatureDatasetPoint fdPoint)
            throws IOException {
        String datasetFileName = new File(fdPoint.getNetcdfFile().getLocation()).getName();
        List<FeatureCollection> featCollList = fdPoint.getPointFeatureCollectionList();

        if (featCollList.size() != 1) {
            throw new IllegalArgumentException(String.format("Expected %s to contain 1 FeatureCollection, not %s.",
                    datasetFileName, featCollList.size()));
        } else if (!(featCollList.get(0) instanceof  StationTimeSeriesFeatureCollection)) {
            String expectedClassName = StationTimeSeriesFeatureCollection.class.getName();
            String actualClassName = featCollList.get(0).getClass().getName();

            throw new IllegalArgumentException(String.format("Expected %s's FeatureCollection to be a %s, not a %s.",
                    datasetFileName, expectedClassName, actualClassName));
        }

        StationTimeSeriesFeatureCollection stationFeatColl = (StationTimeSeriesFeatureCollection) featCollList.get(0);

        if (!stationFeatColl.hasNext()) {
            throw new IllegalArgumentException(String.format("%s's FeatureCollection is empty.",
                    datasetFileName));
        }

        StationTimeSeriesFeature stationFeat = stationFeatColl.next();

        if (stationFeatColl.hasNext()) {
            throw new IllegalArgumentException(String.format("%s's FeatureCollection contains more than 1 feature.",
                    datasetFileName));
        }

        return stationFeat;
    }
}
