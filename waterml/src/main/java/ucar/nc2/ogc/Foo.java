package ucar.nc2.ogc;

import org.xml.sax.SAXException;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;

/**
 * Created by cwardgar on 2014/02/21.
 */
public class Foo {
    public static void main(String[] args) throws URISyntaxException, IOException, NoFactoryFoundException,
            JAXBException, SAXException {
        File pointFile = new File(Foo.class.getResource("/ucar/nc2/ogc/singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
//            StationTimeSeriesFeature stationFeat = NC_OMObservationType.getSingleStationFeatureFromDataset(fdPoint);
//            System.out.println(stationFeat);

            printPointFeatures(fdPoint, System.out);
        } finally {
            fdPoint.close();
        }
    }

    private static void printPointFeatures(FeatureDatasetPoint fdPoint, PrintStream outStream) throws IOException {
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
