/* Copyright */
package thredds.server.catalog.db;

import org.mapdb.*;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

public class TestMapDB {

  public static void main1(String[] args) {
    DB db = DBMaker.newMemoryDB().make();

    ConcurrentNavigableMap treeMap = db.getTreeMap("map");
    treeMap.put("something", "here");

    db.commit();
    db.close();
  }

  public static void main2(String[] args) throws IOException {
    DB db = DBMaker.newMemoryDB().make();
    final ConcurrentNavigableMap treeMap = db.getTreeMap("map");

    long start = System.currentTimeMillis();
    final int[] countDs = {0, 0};

    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all, -1, null, new CatalogCrawler.Listener() {
      public void getDataset(Dataset dd, Object context) {
        countDs[0]++;
        if (countDs[0] % 10000 == 0) System.out.printf("%d ", countDs[0]);
        String path = dd.getUrlPath();
        if (path != null) {
          treeMap.put(path, dd.getFlds());
        }
      }

      public boolean getCatalogRef(CatalogRef dd, Object context) {
        countDs[1]++;
        return true;
      }
    });


    PrintWriter pw = new PrintWriter(System.out);
    int count = 0;

    count += crawler.crawl("file:B:/esgf/ncar/esgcet/catalog.xml", null, null, null);

    treeMap.size();

    /*
    Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
    count += crawler.crawlAllInDirectory(top, false, null, null, null); */

    pw.flush();

    long took = System.currentTimeMillis() - start;
    System.out.printf("took %d msecs%n", took);
    System.out.printf("count %d%n", count);
    System.out.printf("countDs %d%n", countDs[0]);
    System.out.printf("countCatref %d%n", countDs[1]);
    System.out.printf("treeMap.size %d%n", treeMap.size());

    db.commit();
    db.close();
  }

  public static void main(String[] args) {
     long start = System.currentTimeMillis();
     final int[] countDs = {0, 0, 0, 0};

     try {
       String tmp = System.getProperty("java.io.tmpdir");
       String pathname = "C:/temp/mapDBtest/cats.dat";

       File file = new File(pathname);

       DB db = DBMaker.newFileDB(file)
               .mmapFileEnableIfSupported()
               .closeOnJvmShutdown()
               .make();

       final HTreeMap<String, Externalizable> map = db.getHashMap("datasets");
       final Set<String> cats = new HashSet<>();

       CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all, -1, null, new CatalogCrawler.Listener() {
         public void getDataset(Dataset dd, Object context) {
           countDs[0]++;
           if (countDs[0] % 1000 == 0) System.out.printf("%d ", countDs[0]);
           if (countDs[0] % 10000 == 0) System.out.printf("%n");
           String key = dd.getId();
           if (key != null) {
             try {
               if (null != map.get(key)) {
                 // System.out.printf("duplicate id = %s%n", key);
                 countDs[2]++;
                 return;
               }
             } catch (Throwable t) {
               // System.out.printf(" map barf on %s message=%s%n", key, t.getMessage());
               countDs[3]++;
               return;
             }

             DatasetExt dsext = new DatasetExt(dd);
             map.put(key, dsext);
           }
         }

         public boolean getCatalogRef(CatalogRef dd, Object context) {
           if (cats.contains(dd.getXlinkHref())) {
             System.out.printf("duplicate catref %s%n", dd.getXlinkHref());
             return false;
           }

           countDs[1]++;
           cats.add(dd.getXlinkHref());
           return true;
         }
       });


       PrintWriter pw = new PrintWriter(System.out);
       int count = 0;

       //Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
       //count += crawler.crawlAllInDirectory(top, false, null, null, null);

       count += crawler.crawl("file:B:/esgf/ncar/esgcet/catalog.xml", null, null, null);
       // count += crawler.crawl("file:B://esgf/ncar/esgcet/56/ucar.cgd.ccsm4.CESM_CAM5_BGC_LE_COMPROJ.ice.proc.monthly_ave.fswthru.v1.xml", null, null, null);

       /*
       Path top = FileSystems.getDefault().getPath("B:/esgf/ncar/esgcet/1/");
       count += crawler.crawlAllInDirectory(top, false, null, null, null); */

       pw.flush();

       long now = System.currentTimeMillis();
       long took = now - start;
       System.out.printf("%n%nMapDB took %d msecs%n", took);
       System.out.printf("count %d%n", count);
       System.out.printf("countDs %d%n", countDs[0]);
       System.out.printf("countCatref %d%n", countDs[1]);
       System.out.printf("count dups %d%n", countDs[2]);
       System.out.printf("count barf %d%n", countDs[3]);
       // System.out.printf("map.size %d%n", map.size());

       System.out.printf("DatasetExt.total_count %d%n", DatasetExt.total_count);
       System.out.printf("DatasetExt.total_nbytes %d%n", DatasetExt.total_nbytes);
       float avg = DatasetExt.total_nbytes / DatasetExt.total_count;
       System.out.printf("DatasetExt.avg_nbytes %5.0f%n", avg);

       float avg_time = ((float)took) / DatasetExt.total_count;
       System.out.printf(" msecs / record %8.3f%n", avg_time);

       db.commit();
       db.close();

       took = System.currentTimeMillis() - now;
       System.out.printf("%n%n commit took another %d msecs%n", took);

     } catch (IOException e) {
       e.printStackTrace();
     }
   }

}
