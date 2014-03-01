package ucar.nc2.ogc;

import org.xml.sax.SAXException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
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

            PointUtil.printPointFeatures(fdPoint, System.out);
        } finally {
            fdPoint.close();
        }
    }
}
