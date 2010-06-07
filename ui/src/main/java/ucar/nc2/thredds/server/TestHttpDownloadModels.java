package ucar.nc2.thredds.server;

import thredds.catalog.*;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.ui.StopButton;
import ucar.nc2.util.IO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Formatter;

/**
 * Download a sample file from each dataset in idd/models.xml
 *
 * @author caron
 * @since Jun 3, 2010
 */
public class TestHttpDownloadModels implements CatalogCrawler.Listener {
    private String catUrl;
    private int type;

    private InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    private ThreddsDataFactory tdataFactory = new ThreddsDataFactory();

    private StopButton stopButton;
    private JLabel label;
    private PrintStream out;
    private int countDatasets, countNoAccess, countNoOpen;
    private boolean verbose = true;
    private boolean skipDatasetScan = false;
    private String dir;

    TestHttpDownloadModels(String name, String catURL, int type, String dir) throws IOException {
      this.catUrl = catURL;
      this.type = type;
      this.dir = dir;

      JPanel p = new JPanel();
      p.setBorder( BorderFactory.createLineBorder(Color.black ));

      p.add(new JLabel(name+":"));
      label = new JLabel();
      p.add(label);
      stopButton = new StopButton("stopit goddammit!");
      p.add(stopButton);
      main.add( p);

      //FileOutputStream fout = new FileOutputStream(name+".txt");
      out = System.out; // new PrintStream( new BufferedOutputStream( fout));
    }

    public void extract() throws IOException {

      out.println("Read " + catUrl);

      InvCatalogImpl cat = catFactory.readXML(catUrl);
      StringBuilder buff = new StringBuilder();
      boolean isValid = cat.check(buff, false);
      if (!isValid) {
        System.out.println("***Catalog invalid= " + catUrl + " validation output=\n" + buff);
        out.println("***Catalog invalid= " + catUrl + " validation output=\n" + buff);
        return;
      }
      out.println("catalog <" + cat.getName() + "> is valid");
      out.println(" validation output=\n" + buff);

      countDatasets = 0;
      countNoAccess = 0;
      countNoOpen = 0;
      int countCatRefs = 0;
      CatalogCrawler crawler = new CatalogCrawler(type, skipDatasetScan, this);
      long start = System.currentTimeMillis();
      try {
        countCatRefs = crawler.crawl(cat, stopButton, verbose ? out : null, null);
      } finally {
        int took = (int) (System.currentTimeMillis() - start) / 1000;

        out.println("***Done " + catUrl + " took = " + took + " secs\n" +
                "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);

      }
    }

  public void getDataset(InvDataset ds, Object context) {
    countDatasets++;

    InvAccess access = ds.getAccess(ServiceType.HTTPServer);
    if (access == null) return;

    String url = access.getStandardUrlName();
    int pos = url.lastIndexOf("/");
    String filename = dir + url.substring(pos+1);

    out.printf("copy %s to %s ", url, filename);
    IO.readURLtoFile(url, new File(filename));
    out.printf(" OK%n");
  }



    public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }

    public static JPanel main;
    public static void main(String args[]) throws IOException {
      String server = "http://motherlode.ucar.edu:9080/thredds";
      String dir = "Q:/cdmUnitTest/tds/ncep/new/";
      File dirf = new File(dir);
      if (!dirf.exists())
        dirf.mkdir();

      String catalog = "/idd/models.xml";

      JFrame frame = new JFrame("DownloadModels");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      main = new JPanel();
      main.setLayout( new BoxLayout(main, BoxLayout.Y_AXIS));

      TestHttpDownloadModels all_models = new TestHttpDownloadModels("models", server+catalog, CatalogCrawler.USE_RANDOM_DIRECT, dir);
      frame.getContentPane().add(main);
      frame.pack();
      frame.setLocation(40, 300);
      frame.setVisible(true);

      all_models.extract();
    }

  }

