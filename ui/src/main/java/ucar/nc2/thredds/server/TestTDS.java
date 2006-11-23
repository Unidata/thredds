package ucar.nc2.thredds.server;

import ucar.nc2.ui.StopButton;

import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

import thredds.catalog.crawl.CatalogExtractor;
import thredds.catalog.crawl.CatalogCrawler;

import javax.swing.*;

public class TestTDS implements Runnable {
  private String catUrl;
  private int type;

  private CatalogExtractor ce;
  private StopButton stopButton;
  private JLabel label;
  private PrintStream out;

  TestTDS(String name, String catURL, int type) throws IOException {
    this.catUrl = catURL;
    this.type = type;

    JPanel p = new JPanel();
    p.setBorder( BorderFactory.createLineBorder(Color.black ));

    p.add(new JLabel(name+":"));
    label = new JLabel();
    p.add(label);
    stopButton = new StopButton("stopit goddammit!");
    p.add(stopButton);
    main.add( p);

    ce = new CatalogExtractor( false);

    FileOutputStream fout = new FileOutputStream(name+".txt");
    out = System.out; // new PrintStream( new BufferedOutputStream( fout));
  }


  public void run() {
    try {
      ce.extract(out, catUrl, type, false, stopButton);
    } catch (IOException e) {
      e.printStackTrace();
      label.setText("Error");
    }
    out.close();
  }

  public static JPanel main;
  public static void main(String args[]) throws IOException {
    String server = "http://motherlode.ucar.edu:8080/thredds";

    // HEY LOOK
    //ucar.nc2.dods.DODSNetcdfFile.setAllowSessions( true);

    JFrame frame = new JFrame("TestTDS");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    main = new JPanel();
    main.setLayout( new BoxLayout(main, BoxLayout.Y_AXIS));

    TestTDS models = new TestTDS("models", server+"/idd/models.xml", CatalogCrawler.USE_RANDOM_DIRECT);

    /* TestTDS dgex_model = new TestTDS("dgex_model", server+"/idd/dgex_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestTDS gfs_model = new TestTDS("gfs_model", server+"/idd/gfs_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestTDS nam_model = new TestTDS("nam_model", server+"/idd/nam_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestTDS ruc_model = new TestTDS("ruc_model", server+"/idd/ruc_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestTDS ndfd_model = new TestTDS("ndfd_model", server+"/idd/ndfd_model.xml", CatalogCrawler.USE_RANDOM_DIRECT); */ // */

    //TestTDS radar2 = new TestTDS("radar2", "http://motherlode.ucar.edu:8080/thredds/idd/nexrad/level2/catalog.xml", CatalogCrawler.USE_RANDOM);
    //TestTDS radar3 = new TestTDS("radar3", "http://motherlode.ucar.edu:8080/thredds/idd/nexrad/level3/catalog.xml", CatalogCrawler.USE_RANDOM);

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(40, 300);
    frame.setVisible(true);

    /*
    new Thread( dgex_model).start();
    new Thread( gfs_model).start();
    new Thread( nam_model).start();
    new Thread( ruc_model).start();
    new Thread( ndfd_model).start(); // */
    new Thread( models).start(); //

    //new Thread( modelsNc).start();

  }

}
