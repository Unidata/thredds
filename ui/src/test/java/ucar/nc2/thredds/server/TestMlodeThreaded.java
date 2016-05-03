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
package ucar.nc2.thredds.server;

import ucar.nc2.ui.widget.StopButton;

import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

import thredds.catalog.crawl.CatalogExtractor;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.unidata.util.test.TestDir;

import javax.swing.*;

public class TestMlodeThreaded implements Runnable {
  private String catUrl;
  private int type;

  private CatalogExtractor ce;
  private StopButton stopButton;
  private JLabel label;
  private PrintWriter out;

  TestMlodeThreaded(String name, String catURL, int type) throws IOException {
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

    // FileOutputStream fout = new FileOutputStream(name+".txt");
    this.out = new PrintWriter( System.out);
  }


  public void run() {
    try {
      ce.extract(out, catUrl, type, false, stopButton);
    } catch (IOException e) {
      e.printStackTrace();
      label.setText("Error");
    }
    System.out.println("Exit for "+catUrl);
  }

  public static JPanel main;
  public static void main(String args[]) throws IOException {
    String server = "http://"+ TestDir.threddsTestServer+"/thredds";

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

    // TestTDS modelsNc = new TestTDS("modelsNc", server+"/idv/rt-models.1.0.xml", CatalogCrawler.USE_RANDOM_DIRECT);

    TestMlodeThreaded all_models = new TestMlodeThreaded("models", server+"/idd/models.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    /* TestMotherlodeModels gfs_model = new TestMotherlodeModels("gfs_model", server+"/idd/gfs_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestMotherlodeModels nam_model = new TestMotherlodeModels("nam_model", server+"/idd/nam_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestMotherlodeModels ruc_model = new TestMotherlodeModels("ruc_model", server+"/idd/ruc_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);
    TestMotherlodeModels ndfd_model = new TestMotherlodeModels("ndfd_model", server+"/idd/ndfd_model.xml", CatalogCrawler.USE_RANDOM_DIRECT);  // */

    //TestTDS radar2 = new TestTDS("radar2", "http://motherlode.ucar.edu:8080/thredds/idd/nexrad/level2/catalog.xml", CatalogCrawler.USE_RANDOM);
    //TestTDS radar3 = new TestTDS("radar3", "http://motherlode.ucar.edu:8080/thredds/idd/nexrad/level3/catalog.xml", CatalogCrawler.USE_RANDOM);

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(40, 300);
    frame.setVisible(true);

    //new Thread( radar2).start();
    //new Thread( radar3).start();
    all_models.run();
    /* gfs_model.run();
    nam_model.run();
    ruc_model.run();
    ndfd_model.run();
    /* new Thread( gfs_model).start();
    new Thread( nam_model).start();
    new Thread( ruc_model).start();
    new Thread( ndfd_model).start(); // */

    //new Thread( modelsNc).start();

  }

}
