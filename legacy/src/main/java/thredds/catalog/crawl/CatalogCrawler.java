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
 */

public class CatalogCrawler {
  public enum Type {
    all,                         // return all datasets
    all_direct,                  // return all direct datasets, ie that have an access URL
    first_direct,                // return first dataset in each collection of direct datasets
    random_direct,               // return one random dataset in each collection of direct datasets
    random_direct_middle         // return one random dataset in each collection of direct datasets
  }

  // old
  static public final int USE_ALL = 0;
  static public final int USE_ALL_DIRECT = 1;
  static public final int USE_FIRST_DIRECT = 2;
  static public final int USE_RANDOM_DIRECT = 3;
  static public final int USE_RANDOM_DIRECT_NOT_FIRST_OR_LAST = 4;

  private Filter filter = null;
  private Type type = Type.all;
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
    this (Type.values()[type], new FilterDatasetScan(skipDatasetScan), listen);
  }

  /**
   * Constructor.
   *
   * @param type     CatalogCrawler.Type
   * @param filter   dont process this dataset or its descendants. may be null
   * @param listen   each dataset gets passed to the listener
   */
  public CatalogCrawler(Type type, Filter filter, Listener listen) {
    this.type = type;
    this.filter = filter;
    this.listen = listen;

    if (type == Type.random_direct || type == Type.random_direct_middle )
      this.random = new Random(System.currentTimeMillis());
  }

  /**
   * Open a catalog and crawl (depth first) all the datasets in it.
   * Close catalogs and release their resources as you.
   *
   * @param catUrl url of catalog to open
   * @param task   user can cancel the task (may be null)
   * @param out    send status messages to here (may be null)
   * @param context caller can pass this object in (used for thread safety)
   * @return number of catalog references opened and crawled
   */
  public int crawl(String catUrl, CancelTask task, PrintWriter out, Object context) {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);

    if (out != null) {
      out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
      out.println(" validation output=\n" + buff);
    }
    if (isValid)
      return crawl(cat, task, out, context);
    return 0;
  }

  /**
   * Crawl a catalog thats already been opened.
   * When you get to a dataset containing leaf datasets, do all, only the first, or a randomly chosen one.
   *
   * @param cat  the catalog
   * @param task user can cancel the task (may be null)
   * @param out  send status messages to here (may be null)
   * @param context caller can pass this object in (used for thread safety)
   * @return number of catalog references opened and crawled
   */
  public int crawl(InvCatalogImpl cat, CancelTask task, PrintWriter out, Object context) {

    if (out != null)
      out.println("***CATALOG " + cat.getCreateFrom());
    countCatrefs = 0;

    for (InvDataset ds : cat.getDatasets()) {
      if (type == Type.all)
        crawlDataset(ds, task, out, context, true);
      else
        crawlDirectDatasets(ds, task, out, context, true);
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
   * @param context caller can pass this object in (used for thread safety)
   */
  public void crawlDataset(InvDataset ds, CancelTask task, PrintWriter out, Object context, boolean release) {
    boolean isCatRef = (ds instanceof InvCatalogRef);
    if (filter != null && filter.skipAll(ds)) {
      if (isCatRef && release) ((InvCatalogRef) ds).release();
      return;
    }

    boolean isDataScan = ds.findProperty("DatasetScan") != null;

    if (isCatRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      if (out != null)
        out.println(" **CATREF " + catref.getURI() + " (" + ds.getName() + ") ");
      countCatrefs++;

      if (!listen.getCatalogRef( catref, context)) {
        if (release) catref.release();
        return;
      }
    }

    if (!isCatRef || isDataScan)
      listen.getDataset(ds, context);

    // recurse - depth first
      List<InvDataset> dlist = ds.getDatasets();
      if (isCatRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        if (!isDataScan) {
          listen.getDataset(catref.getProxyDataset(), context); // wait till a catref is read, so all metadata is there !
        }
      }

      for (InvDataset dds : dlist) {
        crawlDataset(dds, task, out, context, release);
        if ((task != null) && task.isCancel())
          break;
      }

    if (isCatRef && release) {
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
   * @param context caller can pass this object in (used for thread safety)
   */
  public void crawlDirectDatasets(InvDataset ds, CancelTask task, PrintWriter out, Object context, boolean release) {
    boolean isCatRef = (ds instanceof InvCatalogRef);
    if (filter != null && filter.skipAll(ds)) {
      if (isCatRef && release) ((InvCatalogRef) ds).release();
      return;
    }

    if (isCatRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      if (out != null)
        out.println(" **CATREF " + catref.getURI() + " (" + ds.getName() + ") ");
      countCatrefs++;
      
      if (!listen.getCatalogRef( catref, context)) {
        if (release) catref.release();
        return;
      }
    }

    // get datasets with data access ("leaves")
    List<InvDataset> dlist = ds.getDatasets();
    List<InvDataset> leaves = new ArrayList<InvDataset>();
    for (InvDataset dds : dlist) {
      if (dds.hasAccess())
        leaves.add(dds);
    }

    if (leaves.size() > 0) {
      if (type == Type.first_direct) {
        InvDataset dds = leaves.get(0);
        listen.getDataset(dds, context);

      } else if (type == Type.random_direct) {
        listen.getDataset(chooseRandom(leaves), context);

      } else if (type == Type.random_direct_middle) {
        listen.getDataset(chooseRandomNotFirstOrLast(leaves), context);

      } else { // do all of them
        for (InvDataset dds : leaves) {
          listen.getDataset(dds, context);
          if ((task != null) && task.isCancel()) break;
        }
      }
    }

    // recurse
      for (InvDataset dds : dlist) {
        if (dds.hasNestedDatasets())
          crawlDirectDatasets(dds, task, out, context, release);
        if ((task != null) && task.isCancel())
          break;
      }

    /* if (out != null) {
     int took = (int) (System.currentTimeMillis() - start);
     out.println(" ** " + ds.getName() + " took " + took + " msecs\n");
   } */

    if (ds instanceof InvCatalogRef && release) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      catref.release();
    }

  }

  private InvDataset chooseRandom(List datasets) {
    int index = random.nextInt(datasets.size());
    return (InvDataset) datasets.get(index);
  }

  private InvDataset chooseRandomNotFirstOrLast(List datasets) {
    int index = random.nextInt(datasets.size());
    if ( index == 0 && datasets.size() > 1)
      index++;
    else if ( index == datasets.size() - 1 && datasets.size() > 1)
      index--;
    return (InvDataset) datasets.get(index);
  }

  static public interface Listener {
    /**
     * Gets called for each dataset found.
     * @param dd the dataset
     * @param context caller can pass this object in (used for thread safety)
     */
    public void getDataset(InvDataset dd, Object context);

    /**
     * Gets called for each catalogRef found
     * @param dd the dataset
     * @return true to process, false to skip
     * @param context caller can pass this object in (used for thread safety)
     */
    public boolean getCatalogRef(InvCatalogRef dd, Object context);
  }
  
  static public interface Filter {
    public boolean skipAll(InvDataset ds);
  }
  
  private static class FilterDatasetScan implements Filter {
    boolean skipDatasetScan;

    private FilterDatasetScan(boolean skipDatasetScan) {
      this.skipDatasetScan = skipDatasetScan;
    }

    @Override
    public boolean skipAll(InvDataset ds) {
      return skipDatasetScan && (ds instanceof InvCatalogRef) && (ds.findProperty("DatasetScan") != null);
    }
  }

}
