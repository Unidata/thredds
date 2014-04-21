package ucar.nc2.ogc;

import net.opengis.waterml.x20.CollectionDocument;
import net.opengis.waterml.x20.CollectionType;
import org.apache.xmlbeans.XmlOptions;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc2.waterml.NC_Collection;

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
            VariableSimpleIF dataVar = fdPoint.getDataVariable("tmax");
            CollectionType collection = NC_Collection.createCollection(fdPoint, dataVar);
            CollectionDocument collectionDoc = CollectionDocument.Factory.newInstance();
            collectionDoc.setCollection(collection);

            collectionDoc.save(System.out);
        } finally {
            fdPoint.close();
        }
    }

//    public void marshal(PrintWriter pw) throws Exception {
//        XmlOptions options = makeOptions();
//        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//        document.save(pw, options);
//    }

    private static XmlOptions makeOptions() {
        XmlOptions options = new XmlOptions();

        return options;
    }
}
