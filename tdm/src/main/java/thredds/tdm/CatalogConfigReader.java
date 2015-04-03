package thredds.tdm;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionSpecParser;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import thredds.util.AliasHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Description
 *
 * @author John
 * @since 12/18/13
 */
public class CatalogConfigReader {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogConfigReader.class);
  static private final boolean debug = false;

  private final List<FeatureCollectionConfig> fcList = new ArrayList<>();

  public List<FeatureCollectionConfig> getFcList() {
    return fcList;
  }

  Formatter errlog = new Formatter();
  private AliasHandler aliasHandler;

  public CatalogConfigReader(Resource catR, AliasHandler aliasHandler) throws IOException {
    //FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection("F:/data/grib/idd/modelsNcep.xml#DGEX-CONUS_12km");
    //if (config != null) fcList.add(config);
    this.aliasHandler = aliasHandler;
    readCatalog(catR);
  }

  private boolean readCatalog(Resource catR) throws IOException {
    //String catFilename = catR.getURI().toString();
    //if (catFilename.startsWith("file:/")) catFilename = catFilename.substring("file:/".length());
    File catFile = catR.getFile();
    String fcName = null;

     /* int pos = catalogAndPath.indexOf("#");
     if (pos > 0) {
       catFilename = catalogAndPath.substring(0, pos);
       fcName = catalogAndPath.substring(pos+1);
     } else {
       catFilename = catalogAndPath;
     }  */

    //File cat = new File(catFilename);
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(catFile);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    Element root = doc.getRootElement();


    // find direct fc elements
    try {
      List<Element> fcElems = new ArrayList<>();
      findFeatureCollections(root, fcName, fcElems);
      for (Element fcElem : fcElems) {
        String name = "";
        try {
          FeatureCollectionConfigBuilder configBuilder = new FeatureCollectionConfigBuilder(errlog);
          FeatureCollectionConfig config = configBuilder.readConfig(fcElem);
          if (!configBuilder.fatalError) {
            name = config.collectionName;

            // check root dir exists
            if (config.spec != null)
              config.spec = aliasHandler.replaceAlias(config.spec);
            CollectionSpecParser specp = config.getCollectionSpecParser(errlog);
            Path rootPath = Paths.get(specp.getRootDir());
            if (!Files.exists(rootPath)) {
              System.out.printf("Root path %s does not exist fc='%s' from catalog=%s %n", rootPath.getFileName(), config.collectionName, catFile.getPath());
              log.error("Root path {} does not exist fc='{}' from catalog={}", rootPath.getFileName(), config.collectionName, catFile.getPath());
              continue;
            }

            fcList.add(config);
            if (debug) System.out.printf("Added  fc='%s' from catalog=%s%n", config.collectionName, catFile.getPath());
          }

        } catch (Throwable e) {
          e.printStackTrace();
          log.error("Error reading collection "+name+" skipping collection ", e);
        }
      }

    } catch (Throwable e) {
      e.printStackTrace();
      log.error("Error reading catalog "+catFile.getPath()+" skipping ", e);
    }

    // follow catrefs
    try {
      List<Element> catrefElems = new ArrayList<>();
      findCatalogRefs(root, catrefElems);
      for (Element catrefElem : catrefElems) {
        String href = catrefElem.getAttributeValue("href", Catalog.xlinkNS);
        File refCat = new File(catFile.getParent(), href);
        Resource catRnested = new FileSystemResource(refCat);
        if (!catRnested.exists()) {
          log.error("Relative catalog {} does not exist", refCat);
          continue;
        }
        readCatalog(catRnested);
      }

    } catch (IllegalStateException e) {
      e.printStackTrace();
      log.error("Error follow catrefs in "+catFile.getPath()+" skipping ", e);
    }

    return true;
  }

  private void findFeatureCollections(Element parent, String name, List<Element> fcElems) {
    List<Element> elist = parent.getChildren("featureCollection", Catalog.defNS);
    if (name == null)
      fcElems.addAll(elist);
    else {
      for (Element elem : elist) {
        if (name.equals(elem.getAttributeValue("name")))
          fcElems.add(elem);
      }
    }
    for (Element child : parent.getChildren("dataset", Catalog.defNS))
      findFeatureCollections(child, name, fcElems);
  }

  private void findCatalogRefs(Element parent, List<Element> catrefElems) {
    List<Element> elist = parent.getChildren("catalogRef", Catalog.defNS);
    catrefElems.addAll(elist);

    for (Element child : parent.getChildren("dataset", Catalog.defNS))
      findCatalogRefs(child, catrefElems);
  }
}
