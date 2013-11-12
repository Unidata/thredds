package thredds.catalog.parser.jdom;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;

import java.util.List;

/**
 * Read in the <featureCollection></featureCollection> element
 *
 * @author caron
 * @since 11/11/13
 */
public class FeatureCollectionReader {
  static private final Logger logger = LoggerFactory.getLogger(FeatureCollectionReader.class);

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
    String specName = collElem.getAttributeValue("name");
    if (specName == null) specName = name; // If missing, the Feature Collection name is used.
    String spec = collElem.getAttributeValue("spec");
    // String spec = expandAliasForCollectionSpec(collElem.getAttributeValue("spec")); // LOOK
    String timePartition = collElem.getAttributeValue("timePartition");
    String dateFormatMark = collElem.getAttributeValue("dateFormatMark");
    String olderThan = collElem.getAttributeValue("olderThan");
    String useIndexOnly = collElem.getAttributeValue("useIndexOnly");
    String recheckAfter = collElem.getAttributeValue("recheckAfter");
    if (recheckAfter == null)
       recheckAfter = collElem.getAttributeValue("recheckEvery"); // old name
    if (spec == null) {
      logger.error( "featureCollection "+name+" must have a spec attribute." );
      return null;
    }
    String collName = (specName != null) ? specName : name;
    Element innerNcml = featureCollectionElement.getChild( "netcdf", InvCatalogFactory10.ncmlNS );
    FeatureCollectionConfig config = new FeatureCollectionConfig(collName, path, fcType, spec, dateFormatMark, olderThan, recheckAfter,
            timePartition, useIndexOnly, innerNcml);

    // tds and update elements
    Element tdmElem = featureCollectionElement.getChild( "tdm", InvCatalogFactory10.defNS );
    if (tdmElem != null)
      config.tdmConfig = readUpdateElement( tdmElem);
    Element updateElem = featureCollectionElement.getChild( "update", InvCatalogFactory10.defNS );
    if (updateElem != null)
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
    String startup = updateElem.getAttributeValue("startup");
    String recheckAfter = updateElem.getAttributeValue("recheckAfter");
    String rescan = updateElem.getAttributeValue("rescan");
    String trigger = updateElem.getAttributeValue("trigger");
    String deleteAfter = updateElem.getAttributeValue("deleteAfter");

    return new FeatureCollectionConfig.UpdateConfig(startup, recheckAfter, rescan, trigger, deleteAfter);
  }

}
