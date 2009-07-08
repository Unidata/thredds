/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.ncml;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.RegExpMatchOnPathFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnPathFilter;
import thredds.catalog.ServiceType;

import java.util.*;
import java.io.IOException;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;
import org.jdom.Element;

/**
 * DatasetScanner implements the datasetScan element, to scan for datasets, using a CrawlableDataset.
 *
 * @author caron
 * @since Aug 10, 2007
 */
public class DatasetScannerOld implements Scanner {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetScannerOld.class);

  private CrawlableDataset crawler;
  private CrawlableDatasetFilter filter;
  private boolean wantSubdirs = true;

  // filters
  private long olderThan_msecs; // files must not have been modified for this amount of time (msecs)

  private boolean debugScan = false;

  DatasetScannerOld(Element crawlableDatasetElement, String dirName, String suffix, String regexpPatternString,
                 String subdirsS, String olderS) {

    String crawlerClassName;
    Object crawlerObject = null;

    if (null != crawlableDatasetElement) {
      crawlerClassName = crawlableDatasetElement.getAttributeValue("className");
      crawlerObject = crawlableDatasetElement;
    } else {
      crawlerClassName = "thredds.crawlabledataset.CrawlableDatasetFile";
    }

    try {
      crawler = CrawlableDatasetFactory.createCrawlableDataset(dirName, crawlerClassName, crawlerObject);
    } catch (Exception e) {
      throw new RuntimeException(e.getCause());
    }

    if (null != regexpPatternString)
      filter = new RegExpMatchOnPathFilter(regexpPatternString);
    else if (suffix != null)
      filter = new WildcardMatchOnPathFilter("*" + suffix);

    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        this.olderThan_msecs = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderS);
      }
    }
  }

  //NetcdfDataset.EnhanceMode getEnhanceMode() { return mode; }

  public void scanDirectory(Map<String, CrawlableDataset> map, CancelTask cancelTask) throws IOException {
    scanDirectory(crawler, new Date().getTime(), map, cancelTask);
  }

  private void scanDirectory(CrawlableDataset cd, long now, Map<String, CrawlableDataset> map, CancelTask cancelTask) throws IOException {
    if (!cd.exists() || !cd.isCollection()) {
      logger.error("scanDirectory(): the crawlableDataset to be scanned [" + cd.getPath() + "] does not exist or is not a collection.");
      return;
    }
    List<CrawlableDataset> children = cd.listDatasets();

    for (CrawlableDataset child : children) {
      if (debugScan && filter != null) System.out.println("filter " + child);

      if (child.isCollection() && child.exists()) {
        if (wantSubdirs) scanDirectory(child, now, map, cancelTask);

      } else if ((filter == null) || filter.accept(child)) {

        // dont allow recently modified
        if (olderThan_msecs > 0) {
          Date lastModifiedDate = child.lastModified();
          if (lastModifiedDate != null) {
            long lastModifiedMsecs = lastModifiedDate.getTime();
            if (now - lastModifiedMsecs < olderThan_msecs)
              continue;
          }
        }

        // add to result
        map.put(child.getPath(), child);
        if (debugScan) System.out.println(" accept " + child.getPath());
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  static public void main(String args[]) throws IOException {
    String cat = "http://motherlode.ucar.edu:8080/thredds/catalog/satellite/12.0/WEST-CONUS_4km/20070825/catalog.xml";
    Element config = new Element("config");
    config.setAttribute("className", "thredds.catalog.CrawlableCatalog");

    Element serviceType = new Element("serviceType");
    serviceType.addContent(ServiceType.OPENDAP.toString());

    config.addContent(serviceType);

    DatasetScannerOld crawl = new DatasetScannerOld(config, cat, null, null, "true", null);
    crawl.scanDirectory(new HashMap<String, CrawlableDataset>(), null);
  }
}
