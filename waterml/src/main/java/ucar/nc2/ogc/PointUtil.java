package ucar.nc2.ogc;

import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.List;

/**
 * Created by cwardgar on 2014/02/21.
 */
public class PointUtil {
    /**
     * Opens the dataset at {@code location} as a FeatureDatasetPoint.
     *
     * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
     *                        It should be one of the point types.
     * @param location        the URL or file location of the dataset.
     *                        See {@link ucar.nc2.ft.FeatureDatasetFactoryManager#open}.
     * @return a subclass of FeatureDatasetPoint.
     * @throws java.io.IOException     if an I/O error occurs.
     * @throws NoFactoryFoundException if no {@link ucar.nc2.ft.FeatureDatasetFactory} could be found that can open
     *                                 the dataset at {@code location}.
     */
    public static FeatureDatasetPoint openPointDataset(FeatureType wantFeatureType, String location)
            throws IOException, NoFactoryFoundException {
        Formatter errlog = new Formatter();
        FeatureDataset fDset = FeatureDatasetFactoryManager.open(wantFeatureType, location, null, errlog);

        if (fDset == null) {
            throw new NoFactoryFoundException(errlog.toString());
        } else {
            return (FeatureDatasetPoint) fDset;
        }
    }

    public static StationTimeSeriesFeature getSingleStationFeatureFromDataset(FeatureDatasetPoint fdPoint)
            throws IOException {
        String datasetFileName = new File(fdPoint.getNetcdfFile().getLocation()).getName();
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

    public static void printPointFeatures(FeatureDatasetPoint fdPoint, PrintStream outStream) throws IOException {
        for (FeatureCollection featCol : fdPoint.getPointFeatureCollectionList()) {
            StationTimeSeriesFeatureCollection stationCol = (StationTimeSeriesFeatureCollection) featCol;
            PointFeatureCollectionIterator pointFeatColIter = stationCol.getPointFeatureCollectionIterator(-1);

            while (pointFeatColIter.hasNext()) {
                StationTimeSeriesFeature stationFeat = (StationTimeSeriesFeature) pointFeatColIter.next();
                PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);

                while (pointFeatIter.hasNext()) {
                    PointFeature pointFeature = pointFeatIter.next();
                    StructureData data = pointFeature.getData();

                    for (StructureMembers.Member member : data.getMembers()) {
                        Array memberData = data.getArray(member);
                        outStream.printf("%s: %s    ", member.getName(), memberData);
                    }

                    outStream.println();
                }
            }
        }
    }
}
