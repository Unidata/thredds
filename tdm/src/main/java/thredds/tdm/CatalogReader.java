/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.tdm;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import thredds.catalog.*;
import ucar.nc2.time.CalendarDate;

import java.io.*;
import java.util.*;

/**
 * Taken from DataRootHandler for the TDM.
 *
 * @author caron
 * @since 4/26/11
 */
public class CatalogReader {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogReader.class);

  private final Set<String> staticCatalogHash = new HashSet<String>();
  private final List<InvDatasetFeatureCollection> fcList = new ArrayList<InvDatasetFeatureCollection>();

  /* public CatalogReader(String contentDirectory, String threddsConfigXml) {
    File directory = new File(contentDirectory);
    ThreddsConfig config = new ThreddsConfig(new File(directory, threddsConfigXml));

    ArrayList<String> catList = new ArrayList<String>();
    catList.add("catalog.xml"); // always first
    config.addCatalogRoots(catList);
    log.info("initCatalogs(): initializing " + catList.size() + " root catalogs.");

    for (String catName : catList) {
      try {
        catName = StringUtils.cleanPath(catName);
        log.info("\n**************************************\nCatalog init " + catName + "\n[" + CalendarDate.present() + "]");

        catName = StringUtils.cleanPath(catName);
        File catFile = new File(directory, catName);
        Resource catR = new FileSystemResource(catFile);
        initCatalog(catR, true);

      } catch (Throwable e) {
        log.error("initCatalogs(): Error initializing catalog " + catName + "; " + e.getMessage(), e);
      }
    }
  }  */

  public CatalogReader(Resource catR) {
    try {
      log.info("\n**************************************\nCatalog init " + catR + "\n[" + CalendarDate.present() + "]");
      initCatalog(catR, true);

    } catch (Throwable e) {
      log.error("initCatalogs(): Error initializing catalog " + catR + "; " + e.getMessage(), e);
    }
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
        fcList.add(fc);
      }

      // recurse
      if (!(invDataset instanceof InvCatalogRef)) {
        initSpecialDatasets(invDataset.getDatasets());
      }
    }

  }

  private void initFollowCatrefs(File catFile, List<InvDataset> datasets) throws IOException {
    for (InvDataset invDataset : datasets) {

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc)
              && !(invDataset instanceof InvDatasetFeatureCollection)) {

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

      } else if (!(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc) && !(invDataset instanceof InvDatasetFeatureCollection)) {
        // recurse through nested datasets
        initFollowCatrefs(catFile, invDataset.getDatasets());
      }
    }
  }

  public List<InvDatasetFeatureCollection> getFcList() {
    return fcList;
  }
}
