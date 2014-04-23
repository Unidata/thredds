package ucar.nc2.ogc2;

import net.opengis.gml.x32.*;
import net.opengis.samplingSpatial.x20.ShapeType;
import net.opengis.waterml.x20.CollectionDocument;
import net.opengis.waterml.x20.CollectionType;
import net.opengis.waterml.x20.MonitoringPointType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.oxf.xmlbeans.parser.XMLBeansParser;
import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.NoFactoryFoundException;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc2.waterml.NC_Collection;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by cwardgar on 2014/04/17.
 */
public class Foo {
    public static void main(String[] args) throws IOException, NoFactoryFoundException, XMLHandlingException {
        File pointFile = new File("C:/Users/cwardgar/Desktop/multiStationSingleVar.ncml");
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            VariableSimpleIF dataVar = fdPoint.getDataVariable("tmax");
            CollectionType collection = NC_Collection.createCollection(fdPoint, dataVar);
            CollectionDocument collectionDoc = CollectionDocument.Factory.newInstance();
            collectionDoc.setCollection(collection);

            writeObject(collectionDoc, System.out, false);

//            StationTimeSeriesFeatureCollection stationFeatCollection = NC_Collection.getStationFeatures(fdPoint);
//            stationFeatCollection.resetIteration();
//            StationTimeSeriesFeature stationFeat = stationFeatCollection.next();
//            stationFeat.calcBounds();
//
//            // TODO: Turn on validation.
//            FeaturePropertyType featureOfInterest = NC_FeaturePropertyType.createFeatureOfInterest(stationFeat);
//            writeObject(featureOfInterest, System.out, false);  // TODO: Turn on validation.

//            writeFOI(stationFeat);
        } finally {
            fdPoint.close();
        }
    }

    public static void writeFOI(StationTimeSeriesFeature stationFeat) throws IOException, XMLHandlingException {
        FeaturePropertyDocument featurePropertyDocument = FeaturePropertyDocument.Factory.newInstance();

        FeaturePropertyType featureOfInterest = featurePropertyDocument.addNewFeatureProperty();

        // sam:SF_SamplingFeatureType
        AbstractFeatureType abstractFeatureType = featureOfInterest.addNewAbstractFeature();
        MonitoringPointType sfSamplingFeatureType = MonitoringPointType.Factory.newInstance();
        abstractFeatureType.set(sfSamplingFeatureType);


        // sams:shape
        ShapeType shape = sfSamplingFeatureType.addNewShape();
        PointType point = PointType.Factory.newInstance();
        shape.set(point);

        // gml:pos
        AbstractGeometryType abstractGeometryType = shape.addNewAbstractGeometry();
        DirectPositionType pos = DirectPositionType.Factory.newInstance();
        pos.setListValue(Arrays.asList(
                stationFeat.getLatitude(), stationFeat.getLongitude(), stationFeat.getAltitude()));
        abstractGeometryType.set(pos);


        writeObject(featurePropertyDocument, System.out, false);
    }

    public static void writeObject(XmlObject doc, OutputStream out, boolean validate)
            throws IOException, XMLHandlingException {
        // Add xsi:schemaLocation
        XmlCursor cursor = doc.newCursor();
        if (cursor.toFirstChild()) {
            String location = "http://www.opengis.net/waterml/2.0 http://schemas.opengis.net/waterml/2.0/waterml2.xsd";
            cursor.setAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation"), location);
        }

        if (validate) {
            XMLBeansParser.strictValidate(doc);
        }

        doc.save(out, makeOptions());
    }

    private static XmlOptions makeOptions() {
        XmlOptions options = new XmlOptions();

        options.setSaveNamespacesFirst();
        options.setSavePrettyPrint();
        options.setSavePrettyPrintIndent(4);
        options.setSaveAggressiveNamespaces();

        // This causes "wml2" to be the default. Its elements no longer get prefixed. Not sure if I want this.
        options.setUseDefaultNamespace();

        options.setSaveSuggestedPrefixes(getSuggestedPrefixes());

        options.setSaveImplicitNamespaces(getSuggestedPrefixes());

        return options;
    }

    private static Map<String, String> getSuggestedPrefixes() {
        Map<String, String> prefixMap = new HashMap<>();

        prefixMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        prefixMap.put("http://www.w3.org/1999/xlink", "xlink");
        prefixMap.put("http://www.opengis.net/gml/3.2", "gml");
        prefixMap.put("http://www.opengis.net/om/2.0", "om");
        prefixMap.put("http://www.opengis.net/sampling/2.0", "sam");
        prefixMap.put("http://www.opengis.net/samplingSpatial/2.0", "sams");
        prefixMap.put("http://www.opengis.net/swe/2.0", "swe");
        prefixMap.put("http://www.opengis.net/waterml/2.0", "wml2");
        prefixMap.put("http://www.opengis.net/waterml-dr/2.0", "wml2dr");
        prefixMap.put("http://www.opengis.net/gmlcov/1.0", "gmlcov");

        return prefixMap;
    }
}
