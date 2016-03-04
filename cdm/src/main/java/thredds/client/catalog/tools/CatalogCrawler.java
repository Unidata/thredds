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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.google.common.base.MoreObjects;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Crawl client catalogs
 *
 * @author caron
 * @since 1/11/2015
 */
public class CatalogCrawler {

  public interface Listener {
    /**
     * Gets called for each dataset found.
     *
     * @param dd      the dataset
     * @param context object passed into crawl() by the caller
     */
    void getDataset(Dataset dd, Object context);
  }

  public interface Filter {
    // true means skip this dataset and any children
    boolean skipAll(Dataset ds);

    // true means skip this catref and any children
    boolean skipCatref(CatalogRef ds, int level);
  }

  public enum Type {
    all,                         // return all datasets
    all_direct,                  // return all direct datasets, ie that have an access URL
    first_direct,                // return first dataset in each collection of direct datasets
    random_direct,               // return one random dataset in each collection of direct datasets
    random_direct_middle,        // return one random dataset in each collection of direct datasets
    random_direct_max            // return max random datasets in entire catalog
  }

  private final Type type;
  private final int max;
  private final Filter filter;
  private final Listener listen;
  private final CancelTask task;
  private final PrintWriter out;
  private final Object context;

  private Random random;
  private int countCatrefs = 0;

  private int numReadFailures = 0;

  /**
   * Constructor.
   *
   * @param type   CatalogCrawler.Type
   * @param max    if > 0, only process max datasets, then exit (random_direct_max only)
   * @param filter dont process this dataset or its descendants. may be null
   * @param listen each dataset gets passed to the listener. if null, send the dataset name to standard out
   * @param task    user can cancel the task (may be null)
   * @param out     send status messages to here (may be null)
   * @param context caller can pass this object to Listener (eg used for thread safety)
   */
  public CatalogCrawler(Type type, int max, Filter filter, Listener listen, CancelTask task, PrintWriter out, Object context) {
    this.type = type == null ? Type.all : type;
    this.max = max;
    this.filter = filter;
    this.listen = listen;
    this.task = task;
    this.out = out;
    this.context = context;

    if (type == Type.random_direct || type == Type.random_direct_middle || type == Type.random_direct_max)
      this.random = new Random(System.currentTimeMillis());
  }

  /**
   * Open a catalog and crawl (depth first) all the datasets in it.
   * Any that pass the filter are sent to the Listener
   * Close catalogs and release their resources as you.
   *
   * @param catUrl  url of catalog to open (xml, not html)
   * @return number of catalogs (this + catrefs) opened and crawled
   */
  public int crawl(String catUrl) throws IOException {

    CatalogBuilder catFactory = new CatalogBuilder();
    Catalog cat = catFactory.buildFromLocation(catUrl, null);
    boolean isValid = !catFactory.hasFatalError();
    if (out != null) {
      out.println("Catalog <" + catUrl + "> " + (isValid ? "read ok" : "is not valid"));
      if (!isValid)
        out.println(" validation output=\n" + catFactory.getErrorMessage());
    }

    this.countCatrefs = 0;
    if (isValid)
      return crawl(cat);

    System.err.printf("%s%n", catFactory.getErrorMessage());
    return 0;
  }

  /**
   * Crawl a catalog thats already been opened.
   *
   * @param cat     the catalog
   * @return number of catalog references opened and crawled
   */
  public int crawl(Catalog cat) throws IOException {
    this.countCatrefs = 0;
    crawl(cat, 0, new Indent(2));
    return 1 + countCatrefs;
  }

  private int crawl(Catalog cat, int level, Indent indent) throws IOException {
    for (Dataset ds : cat.getDatasetsLocal()) {
      crawlDataset(ds, level, indent);
      if ((task != null) && task.isCancel()) break;
    }
    return 1 + countCatrefs;
  }

  /**
   * Crawl this dataset recursively.
   *
   * @param ds      the dataset
   * @param level   is the top dataset
   * @param indent  print indentation
   */
  private void crawlDataset(Dataset ds, int level, Indent indent) throws IOException {
    if (filter != null && filter.skipAll(ds))
      return;

    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;

      if (filter != null && filter.skipCatref(catref, level+1))
        return;

      if (out != null) out.printf("%n%sCatalogRef %s (%s)%n", indent, catref.getURI(), ds.getName());
      countCatrefs++;

      Catalog cat = readCatref(catref, out, indent);
      if (cat == null) {
        numReadFailures++;
        return;
      }

      crawl(cat, level+1, indent.incr());
      indent.decr();
      return;
    }

    if (filter != null && filter.skipAll(ds))
      return;

    if (level == 0) {
      if (type == Type.all || ds.hasAccess())
        listen.getDataset(ds, context);
    }

    if (type == Type.all) {
      for (Dataset dds : ds.getDatasetsLocal()) {
        if (!(dds instanceof CatalogRef))
          listen.getDataset(dds, context);
        crawlDataset(dds, level, indent.incr());
        indent.decr();
        if ((task != null) && task.isCancel()) break;
      }

    } else {

      // get datasets with data access ("leaves")
      List<Dataset> dlist = ds.getDatasetsLocal();
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
      // recurse
      for (Dataset dds : ds.getDatasetsLocal()) {
        if (dds.hasNestedDatasets() || (dds instanceof CatalogRef)) {
          crawlDataset(dds, level, indent.incr());
          indent.decr();
          if ((task != null) && task.isCancel()) break;
        }
      }
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

  public int getNumReadFailures() {
    return numReadFailures;
  }


  /* public int crawlAllInDirectory(Path directory, boolean recurse, CancelTask task, PrintWriter out, Object context) throws IOException {
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
  } */

  //////////////////////////////////////////////////////////////////////////////


  private static class FilterDatasetScan implements Filter {
    PrintWriter out;
    boolean skipDatasetScan;
    int catrefLevel;

    int count = 0;
    int countSkip = 0;
    int countCatrefs = 0;

    private FilterDatasetScan(PrintWriter pw, boolean skipDatasetScan, int catrefLevel) {
      this.out = pw;
      this.skipDatasetScan = skipDatasetScan;
      this.catrefLevel = catrefLevel;
    }

    @Override
    public boolean skipAll(Dataset ds) {
      boolean skip = skipDatasetScan && (ds instanceof CatalogRef) && (ds.findProperty("DatasetScan") != null);
      if (skip) {
        countSkip++;
        out.printf("  skip DatasetScan %s%n", ds.getName());
      }
      count++;
      return skip;
    }


    @Override
    public boolean skipCatref(CatalogRef dd, int level) {
      countCatrefs++;
      if (catrefLevel <= 0) return false;
      return level > catrefLevel;
    }
  }

  private static class CommandLine {
    @Parameter(names = {"-cat", "--catalog"}, description = "Top catalog URL", required = true)
    public String topCatalog;

    @Parameter(names = {"-t", "--type"}, description = "type of crawl. Allowed values=" +
            "[all, all_direct, first_direct, random_direct, random_direct_middle, random_direct_max]")
    public Type type = Type.all;

    @Parameter(names = {"-sh", "--showNames"}, description = "show dataset names ")
    public boolean showNames = false;

    @Parameter(names = {"-o", "--openDataset"}, description = "try to open the dataset ")
    public boolean openDataset = false;

    @Parameter(names = {"-r", "--readRandom"}, description = "read some random data")
    public boolean readRandom = false;

    @Parameter(names = {"-skipScans", "--skipScans"}, description = "skip DatasetScans ")
    public boolean skipDatasetScan = true;

    @Parameter(names = {"-catrefLevel", "--catrefLevel"}, description = "skip Catalog References > nested level")
    public int catrefLevel = 0;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;

    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList(
              "--catalog", "--type", "--openDataset", "--skipScans",  "--readRandom", "--catrefLevel", "--showNames", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }

    @Override
    public String toString() {
      return "topCatalog='" + topCatalog + '\'' +
              "\n   type=" + type +
              ", showNames=" + showNames +
              ", skipDatasetScan=" + skipDatasetScan +
              ", catrefLevel=" + catrefLevel +
              ", openDataset=" + openDataset
              ;
    }
  }

  private static class Counter {
    int datasets = 0;
    int openFc = 0;
    int failFc = 0;
    int failException = 0;
    int openOdap = 0;
    int failOdap = 0;
    int openCdmr = 0;
    int failCdmr = 0;
  }

  public static void main(String[] args) throws Exception {
    String progName = CatalogCrawler.class.getName();
    long start = System.currentTimeMillis();
    Counter c = new Counter();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);
      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }
      System.out.printf("%s %n   %s%n", progName, cmdLine);

      PrintWriter pw = new PrintWriter(System.out, true);
      FilterDatasetScan filter = new FilterDatasetScan(pw, cmdLine.skipDatasetScan, cmdLine.catrefLevel);
      CancelTask task = null;

      CatalogCrawler crawler = new CatalogCrawler(cmdLine.type, -1, filter, new Listener() {

        public void getDataset(Dataset dd, Object context) {
          c.datasets++;

          if (cmdLine.showNames) {
            Service s = dd.getServiceDefault();
            String sname = (s == null) ? "none" : s.getName();
            pw.format("  Dataset '%s' service=%s%n", dd.getName(), sname);
          }

          if (cmdLine.openDataset && dd.hasAccess()) {
            Service s = dd.getServiceDefault();
            if (s == null || s.getServiceTypeName().equalsIgnoreCase(ServiceType.HTTPServer.name())) // skip files
              return;

            DataFactory fac = new DataFactory();
            try ( DataFactory.Result result = fac.openFeatureDataset(dd, task)) {
              if (result.fatalError) {
                pw.format("  Dataset fatalError=%s%n", result.errLog);
                c.failFc++;
              } else {
                pw.format("  Dataset '%s' opened as type=%s%n", dd.getName(), result.featureDataset.getFeatureType());
                c.openFc++;
                if (cmdLine.readRandom && result.featureDataset instanceof FeatureDatasetCoverage) {
                  readRandom((FeatureDatasetCoverage) result.featureDataset, pw);
                }
              }
            } catch (IOException e) {
              e.printStackTrace(pw);
              c.failException++;

            } catch (InvalidRangeException e) {
              e.printStackTrace();
              c.failException++;
            }

            // opendap
            Access opendap = dd.getAccess(ServiceType.OPENDAP);
            if (opendap != null) {
              if (readAccess(opendap, fac, pw))
                c.openOdap++;
              else
                c.failOdap++;
            } else {
              // System.out.printf("HEY%n");
            }

            // cdmremote
            Access cdmremote = dd.getAccess(ServiceType.CdmRemote);
            if (cdmremote != null) {
              if (readAccess(cdmremote, fac, pw))
                c.openCdmr++;
              else
                c.failCdmr++;
            }

          }
        }

      }, task, pw, null);

      int count = 0;

      count += crawler.crawl(cmdLine.topCatalog);
      pw.flush();

      long took = System.currentTimeMillis() - start;
      System.out.printf("%nthat took %d msecs%n", took);
      System.out.printf("count catalogs = %d%n", count);
      System.out.printf("count catrefs  = %d%n", filter.countCatrefs);
      System.out.printf("count skipped  = %d%n", filter.countSkip);
      System.out.printf("count filterCalls = %d%n%n", filter.count);

      System.out.printf("             count datasets = %d%n", c.datasets);
      System.out.printf("count open featureCollection = %d%n", c.openFc);
      System.out.printf("count fail featureCollection = %d%n", c.failFc);
      System.out.printf("         count failException = %d%n", c.failException);
      System.out.printf("          count open Opendap = %d%n", c.openOdap);
      System.out.printf("          count fail Opendap = %d%n", c.failOdap);
      System.out.printf("             count open Cdmr = %d%n", c.openCdmr);
      System.out.printf("             count fail Cdmr = %d%n", c.failCdmr);

    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }

  private static boolean readRandom(FeatureDatasetCoverage covDataset, PrintWriter pw ) throws IOException, InvalidRangeException {
    CoverageCollection cc = covDataset.getCoverageCollections().get(0);
    int ncov = cc.getCoverageCount();
    Random r = new Random(System.currentTimeMillis());
    int randomIdx = r.nextInt(ncov);
    int count = 0;
    Coverage randomCov = null;
    for (Coverage c : cc.getCoverages()) {
      if (count == randomIdx) {
        randomCov = c;
        break;
      }
      count++;
    }
    if (randomCov == null) {
      pw.format("Bad random coverage");
      return false;
    }

    SubsetParams subset = new SubsetParams().setTimePresent();
    GeoReferencedArray geo = randomCov.readData(subset);
    Array data = geo.getData();
    System.out.printf(" read data from %s shape = %s%n", randomCov.getName(), Misc.showInts(data.getShape()));
    return true;
  }

  private static boolean readAccess(Access access, DataFactory fac, PrintWriter pw) {
      Formatter log = new Formatter();
      try (NetcdfDataset ncd = fac.openDataset(access, false, null, log)) {
        if (ncd == null) {
          pw.format("  Dataset opendap fatalError=%s%n", log);
          return false;

        } else {
          pw.format("  Dataset '%s' opened as %s%n", access.getDataset().getName(), access.getService());
          return readRandom(ncd, pw);
        }
      } catch (InvalidRangeException | IOException e) {
        e.printStackTrace(pw);
        return false;
      }
  }

  private static boolean readRandom(NetcdfDataset ncd, PrintWriter pw ) throws IOException, InvalidRangeException {
    int ncov = ncd.getVariables().size();
    Random r = new Random(System.currentTimeMillis());
    int randomIdx = r.nextInt(ncov);
    Variable randomVariable = ncd.getVariables().get(randomIdx);

    int[] shape = randomVariable.getShape();
    int[] origin = new int[shape.length];
    int[] size = new int[shape.length];
    for (int i=0; i< shape.length; i++) {
      origin[i] = r.nextInt(shape[i]);
      size[i] = 1;
    }

    Array data = randomVariable.read(origin, size);
    pw.format(" read data from %s origin = %s return = %s%n", randomVariable.getNameAndDimensions(), Misc.showInts(origin),
            NCdumpW.toString(data));
    return true;

  }

}
