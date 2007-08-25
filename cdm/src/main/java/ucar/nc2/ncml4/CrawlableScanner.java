/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ncml4;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.RegExpMatchOnPathFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnPathFilter;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.io.IOException;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;
import org.jdom.Element;
import org.jdom.Document;

/**
 * Use CrawlableDataset to scan for datasets in an aggreggation.
 *
 * @author caron
 * @since Aug 10, 2007
 */
public class CrawlableScanner implements Scanner {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CrawlableScanner.class);

  private CrawlableDataset crawler;
  private CrawlableDatasetFilter filter;

  private String dirName;
  private boolean wantSubdirs = true;

  // filters
  private long olderThan_msecs; // files must not have been modified for this amount of time (msecs)

  private boolean debugScan = true;

  CrawlableScanner(Element crawlableDatasetElement, String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS) {
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
      e.printStackTrace();
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

  public void scanDirectory(List<MyCrawlableDataset> result, CancelTask cancelTask) throws IOException {
    scanDirectory(crawler, new Date().getTime(), result, cancelTask);
  }

  private void scanDirectory(CrawlableDataset cd, long now, List<MyCrawlableDataset> result, CancelTask cancelTask) throws IOException {
    List<CrawlableDataset> children = cd.listDatasets();

    for (CrawlableDataset child : children) {
      //CrawlableDatasetFile cdf = (CrawlableDatasetFile) child;
      //File f = cdf.getFile();

      if (child.isCollection()) {
        if (wantSubdirs) scanDirectory(child, now, result, cancelTask);

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
        MyCrawlableDataset myf = new MyCrawlableDataset(this, child);
        result.add(myf);
        if (debugScan) System.out.println("added " + myf.file.getPath());
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  static public void main(String args[]) throws IOException {
    String cat = "http://motherlode.ucar.edu:8080/thredds/catalog/satellite/12.0/WEST-CONUS_4km/20070825/catalog.xml";
    Element config = new Element("config");
    config.setAttribute("className","thredds.catalog.CrawlableCatalog");
    
    Element serviceType =  new Element("serviceType");
    serviceType.addContent( ServiceType.OPENDAP.toString());

    config.addContent( serviceType);

    CrawlableScanner crawl = new CrawlableScanner(config, cat, null, null, "true", null);
    crawl.scanDirectory(new ArrayList<MyCrawlableDataset>(), null);
  }
}
