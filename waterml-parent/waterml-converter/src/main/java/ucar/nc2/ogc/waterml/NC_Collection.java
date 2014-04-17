package ucar.nc2.ogc.waterml;

import net.opengis.om.v_2_0_0.OMObservationPropertyType;
import net.opengis.waterml.v_2_0_1.CollectionType;
import net.opengis.waterml.v_2_0_1.DocumentMetadataPropertyType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.om.NC_OMObservationPropertyType;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_Collection {
    // wml2:Collection
    public static CollectionType createCollection(FeatureDatasetPoint fdPoint, VariableSimpleIF dataVar)
            throws IOException {
        CollectionType collection = Factories.WATERML.createCollectionType();

        // gml:id
        String id = generateId();
        collection.setId(id);

        // wml2:metadata
        DocumentMetadataPropertyType metadata = NC_DocumentMetadataPropertyType.createMetadata();
        collection.setMetadata(metadata);

        // wml2:observationMember[0..*]
        StationTimeSeriesFeatureCollection stationFeatColl = getStationFeatures(fdPoint);
        PointFeatureCollectionIterator stationFeatCollIter = stationFeatColl.getPointFeatureCollectionIterator(-1);

        while (stationFeatCollIter.hasNext()) {
            // wml2:observationMember
            StationTimeSeriesFeature stationFeat = (StationTimeSeriesFeature) stationFeatCollIter.next();
            OMObservationPropertyType observationMember =
                    NC_OMObservationPropertyType.createObservationMember(fdPoint, stationFeat, dataVar);
            collection.getObservationMembers().add(observationMember);
        }

        return collection;
    }

    public static StationTimeSeriesFeatureCollection getStationFeatures(FeatureDatasetPoint fdPoint) {
        String datasetFileName = new File(fdPoint.getNetcdfFile().getLocation()).getName();

        if (!fdPoint.getFeatureType().equals(FeatureType.STATION)) {
            throw new IllegalArgumentException(String.format("In %s, expected feature type to be STATION, not %s.",
                    datasetFileName, fdPoint.getFeatureType()));
        }

        List<FeatureCollection> featCollList = fdPoint.getPointFeatureCollectionList();

        if (featCollList.size() != 1) {
            throw new IllegalArgumentException(String.format("Expected %s to contain 1 FeatureCollection, not %s.",
                    datasetFileName, featCollList.size()));
        } else if (!(featCollList.get(0) instanceof StationTimeSeriesFeatureCollection)) {
            String expectedClassName = StationTimeSeriesFeatureCollection.class.getName();
            String actualClassName = featCollList.get(0).getClass().getName();

            throw new IllegalArgumentException(String.format("Expected %s's FeatureCollection to be a %s, not a %s.",
                    datasetFileName, expectedClassName, actualClassName));
        }

        return (StationTimeSeriesFeatureCollection) featCollList.get(0);
    }

    private static int numIds = 0;

    private static String generateId() {
        return CollectionType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_Collection() { }
}
