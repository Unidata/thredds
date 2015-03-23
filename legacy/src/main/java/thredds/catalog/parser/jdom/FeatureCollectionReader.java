package thredds.catalog.parser.jdom;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionAbstract;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Read in the <featureCollection> element
 *
 * @author caron
 * @since 11/11/13
 */
public class FeatureCollectionReader {
  static private final Logger logger = LoggerFactory.getLogger(FeatureCollectionReader.class);

    // input is xml file with just the <featureCollection>
  static public FeatureCollectionConfig getConfigFromSnippet(String filename) {

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(filename);
    } catch (Exception e) {
      System.out.printf("Error parsing featureCollection %s err = %s", filename, e.getMessage());
      return null;
    }

    return FeatureCollectionReader.readFeatureCollection(doc.getRootElement());
  }


  /**
   * Read a catalog and extract a FeatureCollectionConfig from it
   * @param catalogAndPath catalog filename, or catalog#featureName
   * @return FeatureCollectionConfig or null
   */
  static public FeatureCollectionConfig readFeatureCollection(String catalogAndPath) {
    String catFilename;
    String fcName = null;

    int pos = catalogAndPath.indexOf("#");
    if (pos > 0) {
      catFilename = catalogAndPath.substring(0, pos);
      fcName = catalogAndPath.substring(pos+1);
    } else {
      catFilename = catalogAndPath;
    }

    File cat = new File(catFilename);
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(cat);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    try {
      List<Element> fcElems = new ArrayList<>();
      findFeatureCollection(doc.getRootElement(), fcName, fcElems);
      if (fcElems.size() > 0)
        return readFeatureCollection(fcElems.get(0));

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

    return null;
  }

  static private void findFeatureCollection(Element parent, String name, List<Element> fcElems) {
    List<Element> elist = parent.getChildren("featureCollection", InvCatalogFactory10.defNS);
    if (name == null)
      fcElems.addAll(elist);
    else {
      for (Element elem : elist) {
        if (name.equals(elem.getAttributeValue("name")))
          fcElems.add(elem);
      }
    }
    for (Element child : parent.getChildren("dataset", InvCatalogFactory10.defNS))
      findFeatureCollection(child, name, fcElems);
  }

  static public FeatureCollectionConfig readFeatureCollection(Element featureCollectionElement) {
    String name = featureCollectionElement.getAttributeValue("name");
    String path = featureCollectionElement.getAttributeValue("path");
    String fcTypeS = featureCollectionElement.getAttributeValue("featureType");

    FeatureCollectionType fcType = FeatureCollectionType.valueOf(fcTypeS);
    if (fcType == null) {
      logger.error( "featureCollection "+name+" must have a valid FeatureCollectionType attribute, found '"+fcTypeS+"'");
      return null;
    }

    // collection element required
    Element collElem = featureCollectionElement.getChild( "collection", InvCatalogFactory10.defNS );
    if (collElem == null) {
      logger.error( "featureCollection "+name+" must have a <collection> element." );
      return null;
    }
    String collectionName = collElem.getAttributeValue("name");
    collectionName = CollectionAbstract.cleanName(collectionName != null ? collectionName : name);

    String spec = collElem.getAttributeValue("spec");
    // String spec = expandAliasForCollectionSpec(collElem.getAttributeValue("spec")); // now done externally
    String timePartition = collElem.getAttributeValue("timePartition");
    String dateFormatMark = collElem.getAttributeValue("dateFormatMark");
    String olderThan = collElem.getAttributeValue("olderThan");
    String rootDir = collElem.getAttributeValue("rootDir");
    String regExp = collElem.getAttributeValue("regExp");
    if (spec == null && rootDir == null) {
      logger.error( "featureCollection "+name+" must have a spec or rootDir attribute." );
      return null;
    }
    Element innerNcml = featureCollectionElement.getChild( "netcdf", InvCatalogFactory10.ncmlNS );
    FeatureCollectionConfig config = new FeatureCollectionConfig(name, path, fcType, spec, collectionName, dateFormatMark, olderThan,
            timePartition, innerNcml);
    config.setFilter(rootDir, regExp);

    // tds and update elements
    Element tdmElem = featureCollectionElement.getChild( "tdm", InvCatalogFactory10.defNS );
    config.tdmConfig = readUpdateElement( tdmElem);
    Element updateElem = featureCollectionElement.getChild( "update", InvCatalogFactory10.defNS );
    config.updateConfig = readUpdateElement( updateElem);

    // protoDataset element
    Element protoElem = featureCollectionElement.getChild( "protoDataset", InvCatalogFactory10.defNS );
    if (protoElem != null) {
      String choice = protoElem.getAttributeValue("choice");
      String change = protoElem.getAttributeValue("change");
      String param = protoElem.getAttributeValue("param");
      Element ncmlElem = protoElem.getChild( "netcdf", InvCatalogFactory10.ncmlNS );
      config.protoConfig = new FeatureCollectionConfig.ProtoConfig(choice, change, param, ncmlElem);
    }

    // fmrcConfig element
    Element fmrcElem = featureCollectionElement.getChild( "fmrcConfig", InvCatalogFactory10.defNS );
    if (fmrcElem != null) {
      String regularize = fmrcElem.getAttributeValue("regularize");
      config.fmrcConfig = new FeatureCollectionConfig.FmrcConfig(regularize);

      String datasetTypes = fmrcElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        config.fmrcConfig.addDatasetType(datasetTypes);

      List<Element> bestElems = fmrcElem.getChildren( "dataset", InvCatalogFactory10.defNS );
      for (Element best : bestElems) {
        String bestName = best.getAttributeValue("name");
        String offs = best.getAttributeValue("offsetsGreaterEqual");
        double off = Double.parseDouble(offs);
        config.fmrcConfig.addBestDataset(bestName, off);
      }
    }

    // pointConfig element optional
    Element pointElem = featureCollectionElement.getChild( "pointConfig", InvCatalogFactory10.defNS );
    if (pointElem != null) {
      String datasetTypes = pointElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        config.pointConfig.addDatasetType(datasetTypes);
    }

    // gribConfig element optional
    Element gribConfig = featureCollectionElement.getChild( "gribConfig", InvCatalogFactory10.defNS );
    if (gribConfig != null) {
      config.gribConfig.configFromXml(gribConfig, InvCatalogFactory10.defNS);
    }

    // done reading - do anything needed
    config.finish();

    return config;
  }

  static private FeatureCollectionConfig.UpdateConfig readUpdateElement(Element updateElem) {
    if (updateElem == null) {
      return new FeatureCollectionConfig.UpdateConfig(); // default
    }

    String startup = updateElem.getAttributeValue("startup");
    String rewrite = updateElem.getAttributeValue("rewrite");
    String recheckAfter = updateElem.getAttributeValue("recheckAfter");
    String rescan = updateElem.getAttributeValue("rescan");
    String trigger = updateElem.getAttributeValue("trigger");
    String deleteAfter = updateElem.getAttributeValue("deleteAfter");

    return new FeatureCollectionConfig.UpdateConfig(startup, rewrite, recheckAfter, rescan, trigger, deleteAfter);
  }

}
