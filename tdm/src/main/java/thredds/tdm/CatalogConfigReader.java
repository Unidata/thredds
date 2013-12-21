package thredds.tdm;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionSpecParser;
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

  private final Set<String> staticCatalogHash = new HashSet<String>();
  private final List<FeatureCollectionConfig> fcList = new ArrayList<>();

  public List<FeatureCollectionConfig> getFcList() {
    return fcList;
  }

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
        FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(fcElem);
        // check spec
        config.spec = aliasHandler.replaceAlias(config.spec);
        Formatter errlog = new Formatter();
        CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
        Path rootPath = Paths.get(specp.getRootDir());
        if (!Files.exists(rootPath)) {
          System.out.printf("Root path %s does not exist fc='%s' from catalog=%s %n", rootPath.getFileName(), config.name, catFile.getPath());
          continue;
        }

        fcList.add(config);
        if (debug) System.out.printf("Added  fc='%s' from catalog=%s%n", config.name, catFile.getPath());
      }

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

    // follow catrefs
    try {
      List<Element> catrefElems = new ArrayList<>();
      findCatalogRefs(root, catrefElems);
      for (Element catrefElem : catrefElems) {
        String href = catrefElem.getAttributeValue("href", InvCatalogFactory10.xlinkNS);
        File refCat = new File(catFile.getParent(), href);
        Resource catRnested = new FileSystemResource(refCat);
        if (!catRnested.exists()) {
          log.error("Relative catalog {} does not exist", catR);
          continue;
        }
        readCatalog(catRnested);
      }

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

    return true;
  }

  private void findFeatureCollections(Element parent, String name, List<Element> fcElems) {
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
      findFeatureCollections(child, name, fcElems);
  }

  private void findCatalogRefs(Element parent, List<Element> catrefElems) {
    List<Element> elist = parent.getChildren("catalogRef", InvCatalogFactory10.defNS);
    catrefElems.addAll(elist);

    for (Element child : parent.getChildren("dataset", InvCatalogFactory10.defNS))
      findCatalogRefs(child, catrefElems);
  }


  /* private void initCatalog(Resource catR, boolean recurse) throws IOException {

    // make sure we dont already have it
    String path = catR.getURI().toString();
    if (staticCatalogHash.contains(path)) { // This method only called by synchronized methods.
      log.warn("initCatalog(): Catalog [" + path + "] already seen, possible loop (skip).");
      return;
    }
    staticCatalogHash.add(path);

    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true); // always validate the config catalogs
    factory.setDataRootLocationAliasExpanders(aliasExpanders);

    InvCatalogImpl cat = readCatalog(factory, catR);
    if (cat == null) {
      log.warn("initCatalog(): failed to read catalog <" + catR + ">.");
      return;
    }

    // look for featureCollections
    initSpecialDatasets(cat.getDatasets());

    if (recurse) {
      try {
        File catFile = catR.getFile();
        if (catFile.exists()) {
          initFollowCatrefs(catFile, cat.getDatasets());
        }
      } catch (IOException ioe) {
        // never mind - not a File
      }
    }
  }

  // read the catalog
  private InvCatalogImpl readCatalog(InvCatalogFactory factory, Resource catR) throws IOException {
    InputStream ios = catR.getInputStream();
    InvCatalogImpl cat = null;
    try {
      cat = factory.readXML(ios, catR.getURI());

      StringBuilder sbuff = new StringBuilder();
      if (!cat.check(sbuff)) {
        log.error("readCatalog(): invalid catalog -- " + sbuff.toString());
        return null;
      }
      log.info("readCatalog(): valid catalog -- " + sbuff.toString());

    } catch (Throwable t) {
      String msg = (cat == null) ? "null catalog" : cat.getLog();
      log.error("readCatalog(): Exception on catalog=" + catR + " " + t.getMessage() + "\n log=" + msg, t);
      return null;

    } finally {
      if (ios != null) {
        try {
          ios.close();
        } catch (IOException e) {
          log.error("readCatalog(): error closing" + catR);
        }
      }
    }

    return cat;
  }

   * Finds datasetScan, datasetFmrc, NcML and restricted access datasets.
   * Look for duplicate Ids (give message). Dont follow catRefs.
   * Only called by synchronized methods.
   *
   * @param dsList the list of InvDatasetImpl
  private void initSpecialDatasets(List<InvDataset> dsList) {

    for (InvDataset invds : dsList) {
      InvDatasetImpl invDataset = (InvDatasetImpl) invds;

      if (invDataset instanceof InvDatasetFeatureCollection) {
        InvDatasetFeatureCollection fc = (InvDatasetFeatureCollection) invDataset;
        //fcList.add(fc);
      }

      // recurse
      if (!(invDataset instanceof InvCatalogRef)) {
        initSpecialDatasets(invDataset.getDatasets());
      }
    }

  }

  private void initFollowCatrefs(File catFile, List<InvDataset> datasets) throws IOException {
    for (InvDataset invDataset : datasets) {

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFeatureCollection)) {

        InvCatalogRef catref = (InvCatalogRef) invDataset;
        String href = catref.getXlinkHref();
        if (log.isDebugEnabled()) log.debug("  catref.getXlinkHref=" + href);

        // Check that catRef is relative
        if (!href.startsWith("http:")) {
          // Clean up relative URLs that start with "./"
          if (href.startsWith("./")) {
            href = href.substring(2);
          }

          File refCat = new File(catFile.getParent(), href);
          Resource catR = new FileSystemResource(refCat);
          if (!catR.exists()) {
            log.error("Reletive catalog {} does not exist", catR);
            continue;
          }
          initCatalog(catR, true);
        }

      } else if (!(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFeatureCollection)) {
        // recurse through nested datasets
        initFollowCatrefs(catFile, invDataset.getDatasets());
      }
    }
  }   */
}
