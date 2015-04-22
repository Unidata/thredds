/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.tools;

import com.google.common.base.MoreObjects;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Crawl client catalogs
 *
 * @author caron
 * @since 1/11/2015
 */
public class CatalogCrawler {

  public enum Type {
    all,                         // return all datasets
    all_direct,                  // return all direct datasets, ie that have an access URL
    first_direct,                // return first dataset in each collection of direct datasets
    random_direct,               // return one random dataset in each collection of direct datasets
    random_direct_middle,        // return one random dataset in each collection of direct datasets
    random_direct_max           // return max random datasets in entire catalog
  }

  private Filter filter = null;
  private int max = -1;
  private Type type = Type.all;
  private Listener listen;

  private Random random;
  private int countCatrefs = 0;

  /**
   * Constructor.
   *
   * @param type   CatalogCrawler.Type
   * @param max    return max (random_direct_max only)
   * @param filter dont process this dataset or its descendants. may be null
   * @param listen each dataset gets passed to the listener
   */
  public CatalogCrawler(Type type, int max, Filter filter, Listener listen) {
    this.type = type;
    this.max = max;
    this.filter = filter;
    this.listen = listen;

    if (type == Type.random_direct || type == Type.random_direct_middle || type == Type.random_direct_max)
      this.random = new Random(System.currentTimeMillis());
  }

  /**
   * Open a catalog and crawl (depth first) all the datasets in it.
   * Close catalogs and release their resources as you.
   *
   * @param catUrl  url of catalog to open
   * @param task    user can cancel the task (may be null)
   * @param out     send status messages to here (may be null)
   * @param context caller can pass this object in (used for thread safety)
   * @return number of catalogs (this + catrefs) opened and crawled
   */
  public int crawl(String catUrl, CancelTask task, PrintWriter out, Object context) throws IOException {

    CatalogBuilder catFactory = new CatalogBuilder();
    Catalog cat = catFactory.buildFromLocation(catUrl, null);
    boolean isValid = !catFactory.hasFatalError();
    if (out != null) {
      out.println("catalog <" + catUrl + "> " + (isValid ? "is" : "is not") + " valid");
      out.println(" validation output=\n" + catFactory.getErrorMessage());
    }
    if (out != null && cat != null)
      out.println("***CATALOG " + cat.getBaseURI());

    if (isValid)
      return crawl(cat, task, out, context, new Indent(2));
    else
      System.err.printf("%s%n", catFactory.getErrorMessage());
    return 0;
  }

  /**
   * Crawl a catalog thats already been opened.
   * When you get to a dataset containing leaf datasets, do all, only the first, or a randomly chosen one.
   *
   * @param cat     the catalog
   * @param task    user can cancel the task (may be null)
   * @param out     send status messages to here (may be null)
   * @param context caller can pass this object in (used for thread safety)
   * @return number of catalog references opened and crawled
   */
  public int crawl(Catalog cat, CancelTask task, PrintWriter out, Object context, Indent indent) throws IOException {

    for (Dataset ds : cat.getDatasets()) {
      crawlDataset(ds, true, task, out, context, indent);
      if ((task != null) && task.isCancel()) break;
    }

    return 1 + countCatrefs;
  }

  /**
   * Crawl this dataset recursively.
   *
   * @param ds      the dataset
   * @param isTop   is the top dataset
   * @param task    user can cancel the task (may be null)
   * @param out     send status messages to here (may be null)
   * @param context caller can pass this object in (used for thread safety)
   * @param indent  print indentation
   */
  private void crawlDataset(Dataset ds, boolean isTop, CancelTask task, PrintWriter out, Object context, Indent indent) throws IOException {
    if (filter != null && filter.skipAll(ds))
      return;

    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;
      if (out != null) out.printf("%s**CATREF %s (%s)%n", indent, catref.getURI(), ds.getName());
      countCatrefs++;

      if (!listen.getCatalogRef(catref, context))
        return;

      Catalog cat = readCatref(catref, out, indent);
      if (cat == null)
        return;

      crawl(cat, task, out, context, indent.incr());
      indent.decr();
      return;
    }

    if (isTop) {
      if (type == Type.all || ds.hasAccess())
        listen.getDataset(ds, context);
    }

    if (type == Type.all) {
      for (Dataset dds : ds.getDatasets()) {
        listen.getDataset(dds, context);
        crawlDataset(dds, false, task, out, context, indent.incr());
        indent.decr();
        if ((task != null) && task.isCancel()) break;
      }

    } else {

      // get datasets with data access ("leaves")
      List<Dataset> dlist = ds.getDatasets();
      List<Dataset> leaves = new ArrayList<>();
      for (Dataset dds : dlist) {
        if (dds.hasAccess())
          leaves.add(dds);
      }

      if (leaves.size() > 0) {
        if (type == Type.first_direct) {
          Dataset dds = leaves.get(0);
          listen.getDataset(dds, context);

        } else if (type == Type.random_direct) {
          listen.getDataset(chooseRandom(leaves), context);

        } else if (type == Type.random_direct_middle) {
          listen.getDataset(chooseRandomNotFirstOrLast(leaves), context);

        } else { // do all of them
          for (Dataset dds : leaves) {
            listen.getDataset(dds, context);
            if ((task != null) && task.isCancel()) break;
          }
        }
      }
    }

    // recurse
    for (Dataset dds : ds.getDatasets()) {
      if (dds.hasNestedDatasets() || (dds instanceof CatalogRef)) {
        crawlDataset(dds, false, task, out, context, indent.incr());
        indent.decr();
        if ((task != null) && task.isCancel()) break;
      }

      if ((task != null) && task.isCancel()) break;
    }
  }

  private Catalog readCatref(CatalogRef catref, PrintWriter out, Indent indent) {
    CatalogBuilder builder = new CatalogBuilder();
    try {
      Catalog cat = builder.buildFromCatref(catref);
      if (builder.hasFatalError() || cat == null) {
        if (out != null) out.printf("%sError reading catref %s err=%s%n", indent, catref.getName(), builder.getErrorMessage());
        return null;
      }
      return cat;
    } catch (IOException e) {
      if (out != null) out.printf("%sError reading catref %s err=%s%n", indent, catref.getName(), e.getMessage());
    }
    return null;
  }


  private Dataset chooseRandom(List datasets) {
    int index = random.nextInt(datasets.size());
    return (Dataset) datasets.get(index);
  }

  private Dataset chooseRandomNotFirstOrLast(List datasets) {
    int index = random.nextInt(datasets.size());
    if (index == 0 && datasets.size() > 1)
      index++;
    else if (index == datasets.size() - 1 && datasets.size() > 1)
      index--;
    return (Dataset) datasets.get(index);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("filter", filter)
            .add("max", max)
            .add("type", type)
            .add("listen", listen)
            .add("random", random)
            .add("countCatrefs", countCatrefs)
            .toString();
  }

  //////////////////////////////////////////////////////////////////////////////

  static public interface Listener {
    /**
     * Gets called for each dataset found.
     *
     * @param dd      the dataset
     * @param context caller can pass this object in (used for thread safety)
     */
    public void getDataset(Dataset dd, Object context);

    /**
     * Gets called for each catalogRef found
     *
     * @param dd      the dataset
     * @param context caller can pass this object in (used for thread safety)
     * @return true to process, false to skip
     */
    public boolean getCatalogRef(CatalogRef dd, Object context);
  }

  static public interface Filter {
    public boolean skipAll(Dataset ds);
  }

  private static class FilterDatasetScan implements Filter {
    boolean skipDatasetScan;

    private FilterDatasetScan(boolean skipDatasetScan) {
      this.skipDatasetScan = skipDatasetScan;
    }

    @Override
    public boolean skipAll(Dataset ds) {
      return skipDatasetScan && (ds instanceof CatalogRef) && (ds.findProperty("DatasetScan") != null);
    }
  }

  public int crawlAllInDirectory(Path directory, boolean recurse, CancelTask task, PrintWriter out, Object context) throws IOException {
    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
      for (Path p : ds) {
        if (Files.isDirectory(p)) {
          if (recurse)
            crawlAllInDirectory(p, recurse, task, out, context);
        } else {
          count += crawl("file:" + p.toString(), null, null, null);
        }
        if ((task != null) && task.isCancel()) break;
      }
    }
    return count;
  }

  public static void main(String[] args) throws IOException {
    long start = System.currentTimeMillis();
    final int[] countDs = {0, 0};

    CatalogCrawler crawler = new CatalogCrawler(Type.all, -1, null, new Listener() {
      public void getDataset(Dataset dd, Object context) {
        countDs[0]++;
        if (countDs[0] % 10000 == 0) System.out.printf("%d ", countDs[0]);
        if (countDs[0] % 100000 == 0) System.out.printf("%n");
      }

      public boolean getCatalogRef(CatalogRef dd, Object context) {
        countDs[1]++;
        return true;
      }
    });

    PrintWriter pw = new PrintWriter(System.out);
    int count = 0;

    //count += crawler.crawl("file:B:/esgf/ncar/esgcet/catalog.xml", null, null, null);
    count += crawler.crawl("file:B:/esgf/gfdl/esgcet/catalog.xml", null, null, null);

    /*
    Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
    count += crawler.crawlAllInDirectory(top, false, null, null, null); */

    pw.flush();

    long took = System.currentTimeMillis() - start;
    System.out.printf("took %d msecs%n", took);
    System.out.printf("count %d%n", count);
    System.out.printf("countDs %d%n", countDs[0]);
    System.out.printf("countCatref %d%n", countDs[1]);
  }
}
