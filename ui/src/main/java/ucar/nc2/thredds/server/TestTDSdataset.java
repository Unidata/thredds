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

import ucar.nc2.ui.StopButton;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import javax.swing.*;
import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class TestTDSdataset implements Runnable {
    private int who;
    private String datasetUrl;
    private StopButton stop;

    TestTDSdataset(int who, String datasetUrl) {
      this.who = who;
      this.datasetUrl = datasetUrl;
      stop = new StopButton("stopit goddammit!");
      main.add(stop);
    }

    public void run() {
      long total = 0, time = 0;
      System.out.println(who+" start");
      try {
          NetcdfFile ncfile = NetcdfDataset.openFile(datasetUrl, stop);
          List vars = ncfile.getVariables();
          for (int i = 0; i < vars.size(); i++) {
            Variable v = (Variable) vars.get(i);
            long size = v.getSize() * v.getElementSize();
            System.out.println(who+" request "+v.getName()+" size = "+size);
            long start = System.currentTimeMillis();
            v.read();
            long took = System.currentTimeMillis() - start;

            double rate = (took == 0) ? 0.0 : size/took/1000.0;
            System.out.println(" ok==="+who+" request "+v.getName()+" size = "+size+" took="+ took+" msec = "+rate+"Mbytes/sec");
            total += size;
            time += took;
            if (stop.isCancel()) break;
          }
          double totald = total / (1000. * 1000.);
          double rate = total/time/1000.0;

          System.out.println(datasetUrl+" == "+who+" total= "+totald+" Mbytes; took = "+time/1000+"secs = "+rate+"Mbytes/sec");
          ncfile.close();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public static JPanel main;
    public static void main(String args[]) throws IOException {

      // HEY LOOK
      //ucar.nc2.dods.DODSNetcdfFile.setAllowSessions( true);

      JFrame frame = new JFrame("TestTDSdataset");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      main = new JPanel();
      main.setLayout( new BoxLayout(main, BoxLayout.Y_AXIS));
      String dataset;

      //String dataset = "http://motherlode.ucar.edu:9080/thredds/dodsC/fmrc/NCEP/NDFD/CONUS_5km/files/NDFD_CONUS_5km_20070502_1200.grib2";
      dataset = "http://motherlode.ucar.edu:9080/thredds/dodsC/modelsNc/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20070501_1200.nc";
      //String dataset = "http://www.gomoos.org/cgi-bin/dods/nph-dods/buoy/dods/A01/A01.accelerometer.historical.nc";
      //dataset="dods://dataportal.ucar.edu:9191/dods/cam3_aquaplanet/run1";  // prob GRADS
      //dataset= "http://ingrid.ldeo.columbia.edu/SOURCES/.CAC/dods";

      DODSNetcdfFile.setAllowCompression(true);
      int n = 1;
      TestTDSdataset[] ta = new TestTDSdataset[n];
      for (int i=0; i<n; i++)
        ta[i] = new TestTDSdataset(i, dataset);

      frame.getContentPane().add(main);
      frame.pack();
      frame.setLocation(40, 300);
      frame.setVisible(true);

      for (int i=0; i<n; i++) {
           Thread t = new Thread( ta[i]);
           t.start();
         }

    }
}
