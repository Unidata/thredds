/* Copyright */
package thredds.server.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.core.*;
import thredds.server.catalog.tracker.*;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 6/5/2015
 */
public class TestEsgCats {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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


  public static void main(String[] args) throws IOException, SecurityException  {
    String str = ".level= SEVERE"; // in memory config for jdk4 logger
    InputStream inputStream = new ByteArrayInputStream(str.getBytes("UTF-8"));
    java.util.logging.LogManager.getLogManager().readConfiguration(inputStream);

    long start = System.nanoTime();

    DatasetTrackerNoop tracker = new DatasetTrackerNoop();
    // tracker.init("C:\\dev\\github\\thredds50\\tds\\src\\test\\content\\thredds\\cache\\catalog", 1000 * 1000);
    // tracker.init("C:/temp/mapDBtest", -1);
    DatasetTracker.Callback callback = new ConfigCatalogInitialization.StatCallback(ConfigCatalogInitialization.ReadMode.always);


    AllowedServices allowedServices = new AllowedServices();
    File contentDir = new File("B:/esgf/gfdl/");
    long maxDatasets = 100;

    //   public ConfigCatalogInitialization(ReadMode readMode, String contentRootPath, String trackerDir, DatasetTracker datasetTracker, CatalogWatcher catalogWatcher,
    //                                         AllowedServices allowedServices, DatasetTracker.Callback callback, long maxDatasets) throws IOException {

    ConfigCatalogInitialization reader = new ConfigCatalogInitialization(ConfigCatalogInitialization.ReadMode.check, contentDir, null,
            tracker, allowedServices, callback, maxDatasets);

    callback.finish();
    tracker.close();

    long now = System.nanoTime();
    long took = now - start;
    System.out.printf("%nRead EsgCats %s took %d msecs%n", contentDir.getPath(), took / 1000 / 1000);
    System.out.printf("%s%n", callback);
  }

}
