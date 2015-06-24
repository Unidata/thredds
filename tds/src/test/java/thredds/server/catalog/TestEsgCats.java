/* Copyright */
package thredds.server.catalog;

import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.core.*;
import thredds.server.catalog.tracker.*;
import ucar.nc2.util.Counters;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 6/5/2015
 */
public class TestEsgCats {

  static class Stats {
    int catrefs;
    int catrefDups;
    int datasets;
    int datasetsWithId;
    int datasetsWithPath;
    int samePath, differPath;

    void show() {
      System.out.printf("   catrefs=%d%n", catrefs);
      System.out.printf("      dups=%d%n", catrefDups);
      System.out.printf("  datasets=%d%n", datasets);
      System.out.printf("     with/withoutId=(%d/%d) ", datasetsWithId, datasets - datasetsWithId);
      System.out.printf("     with/withoutPath=(%d/%d) ", datasetsWithPath, datasets - datasetsWithPath);
      System.out.printf("     same/differ=(%d/%d) ", samePath, differPath);
    }
  }

  static Stats stat = new Stats();
  static boolean show = false;

  /* public static void main2(String[] args) {
    long start = System.nanoTime();

    try {
      final Set<String> cats = new HashSet<>();

      CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all, -1, null, new CatalogCrawler.Listener() {
        public void getDataset(Dataset dd, Object context) {
          stat.datasets++;
          if (stat.datasets % 1000 == 0) System.out.printf("%d ", stat.datasets);
          if (stat.datasets % 10000 == 0) System.out.printf("%n");
          String path = dd.getUrlPath();
          if (path != null) stat.datasetsWithPath++;
          String id = dd.getId();
          if (id != null) stat.datasetsWithId++;
          if (id != null && path != null) {
            if (!id.equals(path)) {
              if (show) System.out.printf(" %s%n %s%n%n", id, path);
              stat.differPath++;
            } else {
              stat.samePath++;
            }
          }
        }

        public boolean getCatalogRef(CatalogRef dd, Object context) {
          if (cats.contains(dd.getXlinkHref())) {
            stat.catrefDups++;
            if (show) System.out.printf("duplicate catref %s%n", dd.getXlinkHref());
            return false;
          }
          if (show) System.out.printf("%s%n", dd.getXlinkHref());

          stat.catrefs++;
          cats.add(dd.getXlinkHref());
          return true;
        }
      });


      PrintWriter pw = new PrintWriter(System.out);
      int crawlCount = 0;

      //Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
      //count += crawler.crawlAllInDirectory(top, false, null, null, null);

      String top = "file:C:/data:/esgf/ncar/esgcet/catalog.xml";
      crawlCount += crawler.crawl(top, null, null, null);
      // count += crawler.crawl("file:B://esgf/ncar/esgcet/56/ucar.cgd.ccsm4.CESM_CAM5_BGC_LE_COMPROJ.ice.proc.monthly_ave.fswthru.v1.xml", null, null, null);

       /*
       Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
       count += crawler.crawlAllInDirectory(top, false, null, null, null);

      pw.flush();

      long now = System.nanoTime();
      long took = now - start;
      System.out.printf("%nRead EsgCats %s took %d msecs%n", top, took / 1000 / 1000);
      System.out.printf(" crawl count %d%n%n", crawlCount);
      float avg_time = ((float) took) / crawlCount / 1000;
      System.out.printf(" microsecs / catalog %8.3f%n", avg_time);
      float avg_time2 = ((float) took) / stat.datasets / 100;
      System.out.printf(" microsecs / dataset %8.3f%n", avg_time2);

      stat.show();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }   */

  static class Stats2 {
    int catrefs;
    int datasets;
    int dataRoot, dataRootFc, dataRootScan, dataRootDir;
    int ncml, ncmlOne;
    int restrict;
    Counters counters = new Counters();

    public Stats2() {
      counters.add("restrict");
      counters.add("nAccess");
      counters.add("serviceType");
      counters.add("ncmlAggSize");
    }

    void show() {
      Formatter f = new Formatter();
      f.format("   catrefs=%d%n", catrefs);
      f.format("  datasets=%d%n", datasets);
      f.format("  restrict=%d%n", restrict);
      f.format("  ncml=%d ncmlOne=%d%n", ncml, ncmlOne);
      f.format("  dataRoot=%d%n", dataRoot);
      f.format("    dataRootFc=%d%n", dataRootFc);
      f.format("    dataRootScan=%d%n", dataRootScan);
      f.format("    dataRootDir=%d%n", dataRootDir);
      f.format("DatasetExt.total_count %d%n", DatasetExt.total_count);
      f.format("DatasetExt.total_nbytes %d%n", DatasetExt.total_nbytes);
      float avg = DatasetExt.total_count == 0 ? 0 : ((float) DatasetExt.total_nbytes) / DatasetExt.total_count;
      f.format("DatasetExt.avg_nbytes %5.0f%n", avg);
      counters.show(f);
      System.out.printf("%s%n", f);
    }
  }

  static Stats2 stat2 = new Stats2();

  public static void main(String[] args) throws IOException, SecurityException  {
    StringBufferInputStream inputStream = new StringBufferInputStream(".level= SEVERE");
    java.util.logging.LogManager.getLogManager().readConfiguration(inputStream);

    long start = System.nanoTime();

    DatasetTracker tracker = new DatasetTrackerNoop();
    // tracker.init("C:\\dev\\github\\thredds50\\tds\\src\\test\\content\\thredds\\cache\\catalog", 1000 * 1000);
    tracker.init("C:/temp/mapDBtest", -1);
    CatalogWatcher catalogWatcher = new CatalogWatcher(tracker, false);

    DataRootPathMatcher dataRootPathMatcher = new DataRootPathMatcher();
    dataRootPathMatcher.setTracker(tracker);

    AllowedServices allowedServices = new AllowedServices();
    String top = "B:/esgf/gfdl/";
    long maxDatasets = -1;
    ConfigCatalogInitialization reader = new ConfigCatalogInitialization(ConfigCatalogInitialization.ReadMode.check, top, "catalog.xml",
            dataRootPathMatcher, tracker, catalogWatcher, allowedServices, new DatasetTracker.Callback() {

      @Override
      public void hasDataRoot(DataRoot dataRoot) {
        stat2.dataRoot++;
        if (dataRoot.getFeatureCollection() != null) stat2.dataRootFc++;
        if (dataRoot.getDatasetScan() != null) stat2.dataRootScan++;
        if (dataRoot.getDirLocation() != null) stat2.dataRootDir++;
      }

      @Override
      public void hasDataset(Dataset ds) {
        stat2.datasets++;
        List<Access> access = ds.getAccess();
        stat2.counters.count("nAccess", access.size());
        for (Access acc : access)
          if (acc.getService() != null) stat2.counters.count("serviceType", acc.getService().toString());
      }

      @Override
      public void hasNcml(Dataset ds) {
        stat2.ncml++;
        org.jdom2.Element netcdfElem = ds.getNcmlElement();
        org.jdom2.Element agg =  netcdfElem.getChild("aggregation", thredds.client.catalog.Catalog.ncmlNS);
        if (agg == null) return;
        List<org.jdom2.Element> nested =  agg.getChildren("netcdf", thredds.client.catalog.Catalog.ncmlNS);
        if (nested == null) return;
        if (nested.size() == 1)
          stat2.ncmlOne++;
        // look for nested ncml - count how many
        stat2.counters.count("ncmlAggSize", nested.size());
      }

      @Override
      public void hasRestriction(Dataset ds) {
        stat2.restrict++;
        String restrict = ds.getRestrictAccess();
        if (restrict != null) stat2.counters.count("restrict", restrict);
      }

      @Override
      public void hasCatalogRef(ConfigCatalog dd) {
        stat2.catrefs++;
        if (stat2.catrefs % 100 == 0) System.out.printf("%d ", stat2.catrefs);
        if (stat2.catrefs % 1000 == 0) System.out.printf("%n");
      }

      public void finish() {}

    }, maxDatasets);

   // catalogWatcher.processEvents(); // this will loop forever, how to interrupt?
   // catalogWatcher.close();
    tracker.close();

    long now = System.nanoTime();
    long took = now - start;
    System.out.printf("%nRead EsgCats %s took %d msecs%n", top, took / 1000 / 1000);
    float avg_time = ((float) took) / stat2.catrefs / 1000;
    System.out.printf(" microsecs / catalog %8.3f%n", avg_time);
    float avg_time2 = ((float) took) / stat2.datasets / 100;
    System.out.printf(" microsecs / dataset %8.3f%n", avg_time2);

    stat2.show();

    System.out.printf("DatasetExt.total_count %d%n", DatasetExt.total_count);
    System.out.printf("DatasetExt.total_nbytes %d%n", DatasetExt.total_nbytes);
    float avg = DatasetExt.total_count == 0 ? 0 : ((float) DatasetExt.total_nbytes) / DatasetExt.total_count;
    System.out.printf("DatasetExt.avg_nbytes %5.0f%n", avg);
  }

}
