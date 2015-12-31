package thredds.tdm;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionSpecParser;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import ucar.nc2.util.AliasTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * TDM catalog reading.
 * We use the jdom2 elements directly, dont use CatalogBuilder(s)
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
  // private AliasHandler aliasHandler;
  private File rootDir;

  public CatalogConfigReader(Path rootPath, Resource catR) throws IOException {
    this.rootDir = rootPath.toFile();
    // this.aliasHandler = aliasHandler;
    readCatalog(catR);
  }

  private boolean readCatalog(Resource catR) throws IOException {
    //String catFilename = catR.getURI().toString();
    //if (catFilename.startsWith("file:/")) catFilename = catFilename.substring("file:/".length());
    File catFile = catR.getFile();
    //String fcName = null;

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
      findNestedElems(root, "featureCollection", fcElems);
      for (Element fcElem : fcElems) {
        String name = "";
        try {
          FeatureCollectionConfigBuilder configBuilder = new FeatureCollectionConfigBuilder(errlog);
          FeatureCollectionConfig config = configBuilder.readConfig(fcElem);
          if (!configBuilder.fatalError) {
            name = config.collectionName;

            if (config.spec != null)
              config.spec = AliasTranslator.translateAlias(config.spec);

            CollectionSpecParser specp = config.getCollectionSpecParser(errlog);
            Path rootPath = Paths.get(specp.getRootDir());
            if (!Files.exists(rootPath)) {
              System.out.printf("Root path '%s' does not exist fc='%s' from catalog=%s %n", rootPath.toString(), config.collectionName, catFile.getPath());
              log.error("Root path '{}' does not exist fc='{}' from catalog={}", rootPath.toString(), config.collectionName, catFile.getPath());
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
      log.error("Error reading catalog " + catFile.getPath() + " skipping ", e);
    }

    // follow catrefs
    try {
      List<Element> catrefElems = new ArrayList<>();
      findNestedElems(root, "catalogRef", catrefElems);
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

    // follow catscans
    try {
      List<Element> catscanElems = new ArrayList<>();
      findNestedElems(root, "catalogScan", catscanElems);
      for (Element catscanElem : catscanElems) {
        String location = catscanElem.getAttributeValue("location");
        File catDir = new File(rootDir, location);      // LOOK location reletive to rootDir, could be to current dir
        if (!catDir.exists()) {
          log.error("Catalog scan directory {} does not exist", catDir);
          continue;
        }
        readCatsInDirectory(catDir.toPath());
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
      log.error("Error follow catrefs in "+catFile.getPath()+" skipping ", e);
    }

    return true;
  }

  private void findNamedNestedElems(Element parent, String elementName, String attName, List<Element> result) {
    List<Element> elist = parent.getChildren(elementName, Catalog.defNS);
    for (Element elem : elist) {
      if (attName.equals(elem.getAttributeValue("name")))
        result.add(elem);
    }

    for (Element child : parent.getChildren("dataset", Catalog.defNS))
      findNamedNestedElems(child, elementName, attName, result);
  }

  private void findNestedElems(Element parent, String elementName, List<Element> result) {
    List<Element> elist = parent.getChildren(elementName, Catalog.defNS);
    result.addAll(elist);

    for (Element child : parent.getChildren("dataset", Catalog.defNS))
      findNestedElems(child, elementName, result);
  }

  private void readCatsInDirectory(Path catDir) throws IOException {

     // do any catalogs first
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(catDir, "*.xml")) {
      for (Path p : ds) {
        if (!Files.isDirectory(p)) {
          Resource catRnested = new FileSystemResource(p.toFile());
          readCatalog(catRnested);
        }
      }
    }

    // now recurse into the directories
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(catDir)) {
       for (Path dir : ds) {
         if (Files.isDirectory(dir)) {
           readCatsInDirectory(dir);
         }
       }
     }
   }

}
