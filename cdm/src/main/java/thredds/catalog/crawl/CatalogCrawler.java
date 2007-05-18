// $Id: CatalogCrawler.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.catalog.crawl;

import ucar.nc2.util.CancelTask;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;


/**
 * This crawls a catalog tree for its datasets, which are sent to a listener.
 * You can get all or some of the datasets.
 * A "direct" dataset is one which hasAccess() is true, meaning it has one or more access elements.
 * <p/>
 * Example use:
 * <pre>
 * CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
 *   public void getDataset(InvDataset dd) {
 *     if (dd.isHarvest())
 *       doHarvest(dd);
 *   }
 * };
 * CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, listener);
 * </pre>
 *
 * @author John Caron
 * @version $Id: CatalogCrawler.java 48 2006-07-12 16:15:40Z caron $
 */

public class CatalogCrawler {
  /**
   * return all datasets
   */
  static public final int USE_ALL = 0;
  /**
   * return all direct datasets, ie that have an access URL
   */
  static public final int USE_ALL_DIRECT = 1;
  /**
   * return first dataset in each collection of direct datasets.
   */
  static public final int USE_FIRST_DIRECT = 2;
  /**
   * return one random dataset in each collection of direct datasets.
   */
  static public final int USE_RANDOM_DIRECT = 3;

  private boolean skipDatasetScan = false;
  private int type = USE_ALL;
  private Listener listen;

  private Random random;
  private int countCatrefs;

  /**
   * Constructor.
   *
   * @param type            CatalogCrawler.USE_XXX constant: When you get to a dataset containing leaf datasets,
   *                        do all, only the first, or a randomly chosen one.
   * @param skipDatasetScan if true, dont recurse into DatasetScan elements. This is
   *                        useful if you are looking only for collection level metadata.
   * @param listen          this is called for each dataset.
   */
  public CatalogCrawler(int type, boolean skipDatasetScan, Listener listen) {
    this.type = type;
    this.skipDatasetScan = skipDatasetScan;
    this.listen = listen;

    if (type == USE_RANDOM_DIRECT)
      this.random = new Random(System.currentTimeMillis());
  }

  /**
   * Open a catalog and crawl (depth first) all the datasets in it.
   * Close catalogs and release their resources as you.
   *
   * @param catUrl url of catalog to open
   * @param task   user can cancel the task (may be null)
   * @param out    send status messages to here (may be null)
   * @return number of catalog references opened and crawled
   */
  public int crawl(String catUrl, CancelTask task, PrintStream out) {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuffer buff = new StringBuffer();
    boolean isValid = cat.check(buff, false);

    if (out != null) {
      out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
      out.println(" validation output=\n" + buff);
    }
    if (isValid)
      return crawl(cat, task, out);
    return 0;
  }

  /**
   * Crawl a catalog thats already been opened.
   * When you get to a dataset containing leaf datasets, do all, only the first, or a randomly chosen one.
   *
   * @param cat  the catalog
   * @param task user can cancel the task (may be null)
   * @param out  send status messages to here (may be null)
   * @return number of catalog references opened and crawled
   */
  public int crawl(InvCatalogImpl cat, CancelTask task, PrintStream out) {

    if (out != null)
      out.println("***CATALOG " + cat.getCreateFrom());
    countCatrefs = 0;

    List datasets = cat.getDatasets();
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset ds = (InvDataset) datasets.get(i);
      if (type == USE_ALL)
        crawlDataset(ds, task, out);
      else
        crawlDirectDatasets(ds, task, out);
      if ((task != null) && task.isCancel()) break;
    }

    return 1 + countCatrefs;
  }

  /**
   * Crawl this dataset recursively, return all datasets
   *
   * @param ds   the dataset
   * @param task user can cancel the task (may be null)
   * @param out  send status messages to here (may be null)
   */
  public void crawlDataset(InvDataset ds, CancelTask task, PrintStream out) {
    boolean isCatRef = (ds instanceof InvCatalogRef);
    boolean isDataScan = ds.findProperty("DatasetScan") != null;
    boolean skipScanChildren = skipDatasetScan && (ds instanceof InvCatalogRef) && isDataScan;

    if (isCatRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      if (out != null)
        out.println(" **CATREF " + catref.getURI() + " (" + ds.getName() + ") ");
      countCatrefs++;
    }

    if (!isCatRef || skipScanChildren) listen.getDataset(ds);

    // recurse - depth first
    if (!skipScanChildren) {
      java.util.List dlist = ds.getDatasets();
      if (isCatRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        if (isDataScan) {
          listen.getDataset(catref); // wait till a catref is read, so all metadata is there !
        } else {
          listen.getDataset(catref.getProxyDataset()); // wait till a catref is read, so all metadata is there !
        }
      }

      for (int i = 0; i < dlist.size(); i++) {
        InvDataset dds = (InvDataset) dlist.get(i);
        crawlDataset(dds, task, out);
        if ((task != null) && task.isCancel())
          break;
      }
    }

    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      catref.release();
    }

  }

  /**
   * Crawl this dataset recursively. Only send back direct datasets
   *
   * @param ds   the dataset
   * @param task user can cancel the task (may be null)
   * @param out  send status messages to here (may be null)
   */
  public void crawlDirectDatasets(InvDataset ds, CancelTask task, PrintStream out) {
    boolean isCatRef = (ds instanceof InvCatalogRef);
    boolean skipScanChildren = skipDatasetScan && (ds instanceof InvCatalogRef) && (ds.findProperty("DatasetScan") != null);

    if (isCatRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      if (out != null)
        out.println(" **CATREF " + catref.getURI() + " (" + ds.getName() + ") ");
      countCatrefs++;
    }

    // get datasets with data access ("leaves")
    java.util.List dlist = ds.getDatasets();
    ArrayList leaves = new ArrayList();
    for (int i = 0; i < dlist.size(); i++) {
      InvDataset dds = (InvDataset) dlist.get(i);
      if (dds.hasAccess()) leaves.add(dds);
    }

    if (leaves.size() > 0) {
      if (type == USE_FIRST_DIRECT) {
        InvDataset dds = (InvDataset) leaves.get(0);
        listen.getDataset(dds);

      } else if (type == USE_RANDOM_DIRECT) {
        listen.getDataset(chooseRandom(leaves));

      } else { // do all of them
        for (int i = 0; i < leaves.size(); i++) {
          InvDataset dds = (InvDataset) leaves.get(i);
          listen.getDataset(dds);
          if ((task != null) && task.isCancel()) break;
        }
      }
    }

    // recurse
    if (!skipScanChildren) {
      for (int i = 0; i < dlist.size(); i++) {
        InvDataset dds = (InvDataset) dlist.get(i);
        if (dds.hasNestedDatasets())
          crawlDirectDatasets(dds, task, out);
        if ((task != null) && task.isCancel())
          break;
      }
    }

    /* if (out != null) {
     int took = (int) (System.currentTimeMillis() - start);
     out.println(" ** " + ds.getName() + " took " + took + " msecs\n");
   } */

    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      catref.release();
    }

  }

  private InvDataset chooseRandom(List datasets) {
    int index = random.nextInt(datasets.size());
    return (InvDataset) datasets.get(index);
  }

  static public interface Listener {
    /**
     * Gets called for each dataset.
     */
    public void getDataset(InvDataset dd);
  }

}
