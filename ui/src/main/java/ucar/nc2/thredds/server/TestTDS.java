package ucar.nc2.thredds.server;

import ucar.nc2.ui.StopButton;

import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

import thredds.catalog.crawl.CatalogExtractor;
import thredds.catalog.crawl.CatalogCrawler;

import javax.swing.*;

/**
 * Test reading datasets from some catalog
 */
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
    //String server = "http://motherlode.ucar.edu:9080/thredds";
    String server = "http://localhost:8080/thredds/";

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

    // TestTDS models = new TestTDS("models", server+"/idd/models.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestTDS fmrc = new TestTDS("fmrc", server+"catalog/fmrc/namExtract/catalog.xml", CatalogCrawler.USE_ALL_DIRECT);

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(40, 300);
    frame.setVisible(true);

    new Thread( fmrc).start(); //
  }

}
