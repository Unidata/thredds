package ucar.nc2.thredds.server;

import ucar.nc2.ui.StopButton;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import javax.swing.*;
import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Dec 14, 2005
 * Time: 1:00:11 PM
 * To change this template use File | Settings | File Templates.
 */
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
      System.out.println(who+" start");
      try {
        while(true) {
          NetcdfFile ncfile = NetcdfDataset.openFile(datasetUrl, stop);
          if (stop.isCancel()) break;
          List vars = ncfile.getVariables();
          for (int i = 0; i < vars.size(); i++) {
            Variable v = (Variable) vars.get(i);
            v.read();
            if (stop.isCancel()) break;
            //System.out.println(who+" "+v.getName()+" ok");
          }
          System.out.println(who+" done");
          ncfile.close();
        }

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

      int n = 5;
      TestTDSdataset[] ta = new TestTDSdataset[n];
      for (int i=0; i<n; i++)
        ta[i] = new TestTDSdataset(i, "dods://motherlode.ucar.edu:8080/thredds/dodsC/modelsNc/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20051214_0000.nc");

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
