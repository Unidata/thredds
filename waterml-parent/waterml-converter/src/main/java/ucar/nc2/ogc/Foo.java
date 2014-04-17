package ucar.nc2.ogc;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ogc.waterml.NC_Collection;
import ucar.nc2.ogc2.gml.NC_TimePeriodType;

import java.io.File;
import java.io.IOException;


/**
 * Created by cwardgar on 2014/04/17.
 */
public class Foo {
    public static void main(String[] args) throws IOException, NoFactoryFoundException {
        File pointFile = new File("C:/Users/cwardgar/Desktop/multiStationSingleVar.ncml");
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            StationTimeSeriesFeatureCollection stationFeatColl = NC_Collection.getStationFeatures(fdPoint);
            PointFeatureCollectionIterator stationFeatCollIter = stationFeatColl.getPointFeatureCollectionIterator(-1);
            StationTimeSeriesFeature stationFeat = (StationTimeSeriesFeature) stationFeatCollIter.next();
            stationFeat.calcBounds();

            System.out.println(NC_TimePeriodType.createTimePeriod(stationFeat));
        } finally {
            fdPoint.close();
        }
    }
}
