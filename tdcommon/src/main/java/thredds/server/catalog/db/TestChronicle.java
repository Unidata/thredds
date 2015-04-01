/* Copyright */
package thredds.server.catalog.db;

import net.openhft.chronicle.map.*;
import net.openhft.lang.io.serialization.BytesMarshallable;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.CatalogCrawler;
import thredds.server.catalog.proto.DatasetExt;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

/**
 * Describe
 *
 * @author caron
 * @since 3/28/2015
 */
public class TestChronicle {

  public static void main1(String[] args) {

    try {
      String tmp = System.getProperty("java.io.tmpdir");
      String pathname = "C:/temp/chronicleTest/myfile.dat";

      File file = new File(pathname);

      ChronicleMapBuilder<Long, CharSequence> builder = ChronicleMapBuilder.of(Long.class, CharSequence.class)
              .averageValueSize(500).entries(1100 * 1000);

      ChronicleMap<Long, CharSequence> map = builder.createPersistedTo(file);

      Random r = new Random();
      for (int counter = 0; counter < 1000; counter++) {
        long key = r.nextLong();
        byte[] value = new byte[1000];
        r.nextBytes(value);

        map.put(key, new String(value));
      }
      System.out.printf("Map size: %,d %n", map.longSize());


    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    final int[] countDs = {0, 0};

    try {
      String tmp = System.getProperty("java.io.tmpdir");
      String pathname = "C:/temp/chronicleTest/cats.dat";

      File file = new File(pathname);

      ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
              .averageValueSize(1000).entries(1100 * 1000);

      final ChronicleMap<String, Externalizable> map = builder.createPersistedTo(file);

      final int chunk = 10000;
      final int chunk10 = 100000;
      CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all, -1, null, new CatalogCrawler.Listener() {
        public void getDataset(Dataset dd, Object context) {
          countDs[0]++;
          if (countDs[0] % chunk == 0) System.out.printf("%d ", countDs[0]);
          if (countDs[0] % chunk10 == 0) System.out.printf("%n");
          String key = dd.getId();
          if (key != null) {
            DatasetExt dsext = new DatasetExt(dd);
            map.put(key, dsext);
          }
        }

        public boolean getCatalogRef(CatalogRef dd, Object context) {
          countDs[1]++;
          return true;
        }
      });


      PrintWriter pw = new PrintWriter(System.out);
      int count = 0;

      //Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
      //count += crawler.crawlAllInDirectory(top, false, null, null, null);

      // count += crawler.crawl("file:B:/esgf/ncar/esgcet/catalog.xml", null, null, null);
      // count += crawler.crawl("file:B://esgf/ncar/esgcet/56/ucar.cgd.ccsm4.CESM_CAM5_BGC_LE_COMPROJ.ice.proc.monthly_ave.fswthru.v1.xml", null, null, null);


      //Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
      //count += crawler.crawlAllInDirectory(top, false, null, null, null);
      count += crawler.crawl("file:B:/esgf/gfdl/esgcet/catalog.xml", null, null, null);

      pw.flush();

      long took = System.currentTimeMillis() - start;
      System.out.printf("%n%nChronicle took %d msecs%n", took);
      System.out.printf("count %d%n", count);
      System.out.printf("countDs %d%n", countDs[0]);
      System.out.printf("countCatref %d%n", countDs[1]);
      System.out.printf("map.size %d%n", map.size());

      System.out.printf("DatasetExt.total_count %d%n", DatasetExt.total_count);
      System.out.printf("DatasetExt.total_nbytes %d%n", DatasetExt.total_nbytes);
      float avg = DatasetExt.total_nbytes / DatasetExt.total_count;
      System.out.printf("DatasetExt.avg_nbytes %5.0f%n", avg);

      float avg_time = ((float)took) / DatasetExt.total_count;
      System.out.printf(" msecs / record %8.3f%n", avg_time);

      map.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
