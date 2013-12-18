package thredds.tdm;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import thredds.catalog.*;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.util.PathAliasReplacement;
import ucar.nc2.time.CalendarDate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description
 *
 * @author John
 * @since 12/18/13
 */
public class CatalogConfigReader {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogReader.class);

  private final Set<String> staticCatalogHash = new HashSet<String>();
  private final List<FeatureCollectionConfig> fcList = new ArrayList<>();

  public List<FeatureCollectionConfig> getFcList() {
    return fcList;
  }

  private List<PathAliasReplacement> aliasExpanders;
  public CatalogConfigReader(Resource catR, List<PathAliasReplacement> aliasExpanders) {
    this.aliasExpanders = aliasExpanders;
    try {
      log.info("\n**************************************\nCatalog init " + catR + "\n[" + CalendarDate.present() + "]");
      initCatalog(catR, true);

    } catch (Throwable e) {
      log.error("initCatalogs(): Error initializing catalog " + catR + "; " + e.getMessage(), e);
    }
  }

  public CatalogConfigReader() {
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection("F:/data/grib/idd/modelsNcep.xml#DGEX-CONUS_12km");
    fcList.add(config);
  }

  private void initCatalog(Resource catR, boolean recurse) throws IOException {

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
      } catch (IOException ioe ) {
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

  /**
   * Finds datasetScan, datasetFmrc, NcML and restricted access datasets.
   * Look for duplicate Ids (give message). Dont follow catRefs.
   * Only called by synchronized methods.
   *
   * @param dsList the list of InvDatasetImpl
   */
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
  }
}
