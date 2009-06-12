package ucar.nc2.stream;

import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointDatasetRemote;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

/**
 * Describe
 *
 * @author caron
 * @since May 19, 2009
 */
public class CdmRemoteDatasetFactory {
  static private boolean showXML = true;

  static public FeatureDataset factory(FeatureType wantFeatureType, String endpoint) throws IOException {
    if (endpoint.startsWith(ucar.nc2.stream.NcStreamRemote.SCHEME))
      endpoint = endpoint.substring(ucar.nc2.stream.NcStreamRemote.SCHEME.length());

    Document doc = getCapabilities(endpoint);
    Element root = doc.getRootElement();
    Element elem = root.getChild("featureDataset");
    String fType = elem.getAttribute("type").getValue();
    String datasetUri = elem.getAttribute("url").getValue();
    
    System.out.printf("CdmRemoteDatasetFactory endpoint %s getCapabilities= %s %s%n", endpoint, fType, datasetUri);

    FeatureType ft = FeatureType.valueOf(fType);
    NcStreamRemote ncremote = new NcStreamRemote(datasetUri, null);
    NetcdfDataset ncd = new NetcdfDataset(ncremote, null);

    if (ft == null || ft == FeatureType.GRID) {
      return new GridDataset(ncd);
    } else {
      return new PointDatasetRemote(ft, ncd, ncremote);
    }
  }

  static private org.jdom.Document getCapabilities(String endpoint) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(false);
      doc = builder.build(endpoint+"?getCapabilities"); // LOOK - not useing httpclient
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    if (showXML) {
      System.out.printf("*** endpoint = %s %n", endpoint);
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.printf("*** NetcdfDataset/showParsedXML = %n %s %n", xmlOut.outputString(doc));
    }

    return doc;
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/ncstream/point/data";
    FeatureDatasetPoint fd = (FeatureDatasetPoint) CdmRemoteDatasetFactory.factory(FeatureType.ANY, endpoint);
    PointFeatureCollection pc = (PointFeatureCollection) fd.getPointFeatureCollectionList().get(0);

    PointFeatureIterator pfIter = pc.getPointFeatureIterator(-1);
    try {
      while (pfIter.hasNext()) {
        PointFeature pf = pfIter.next();
        System.out.println("pf= " + pf);
      }
    } finally {
      pfIter.finish();
    }
  }


}
