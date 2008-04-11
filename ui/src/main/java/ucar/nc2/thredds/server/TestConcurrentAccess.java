package ucar.nc2.thredds.server;

import ucar.nc2.ui.StopButton;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.NCdump;
import ucar.nc2.util.IO;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import javax.swing.*;
import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.util.List;

public class TestConcurrentAccess {


  private StopButton stopButton;
  private JLabel label;

  TestConcurrentAccess(String name) throws IOException {

    JPanel p = new JPanel();
    p.setBorder(BorderFactory.createLineBorder(Color.black));

    p.add(new JLabel(name + ":"));
    label = new JLabel();
    p.add(label);
    stopButton = new StopButton("stop");
    p.add(stopButton);
    main.add(p);
  }

  /////////////////////////////////////////////
  // read an opendap dataset

  private static class FetchOpendapDataset implements Runnable {
    private String name;
    private int who;

    FetchOpendapDataset(String name, int who) {
     this.name = name;
     this.who = who;
    }
    public void run() {
      NetcdfDataset ncd = null;
      try {
        ncd = NetcdfDataset.openDataset(name);
        System.out.println(who + " Open " + name);
        List vars = ncd.getVariables();
        for (int i = 0; i < vars.size(); i++) {
          Variable v = (Variable) vars.get(i);
          System.out.println(" " + who + " " + v.getName());
          read(v);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InvalidRangeException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } finally {
        if (ncd != null) try {
          ncd.close();
        } catch (IOException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }

    private void read(Variable v) throws IOException, InvalidRangeException {
      int[] shape = v.getShape();
      int[] origin = v.getShape();
      for (int i = 0; i < shape.length; i++) {
        origin[i] = shape[i] / 2;
        shape[i] = 1;
      }
      Array data = v.read(origin, shape);
      NCdump.printArray(data, who + " " + v.getName(), System.out, null);
    }
  }

    /////////////////////////////////////////////
  // read an opendap dataset

  private static class ReadCatalog implements Runnable {
    private String name;
    private int who;

    ReadCatalog(String name, int who) {
     this.name = name;
     this.who = who;
    }
    public void run() {
      System.out.println(who + " readCat " + name);
      String content = IO.readURLcontents(name);
      System.out.println(who + " readCat done "+content.length());
    }

  }
  //////////////////////////////////////////////////////////////

  public static JPanel main;

  public static void main(String args[]) throws IOException {
    String dodsName = "http://localhost:8080/thredds/dodsC/fmrc/NAM-CONUS-12/Formica-NAM-CONUS-12_best.ncd";
    String catName = "http://motherlode:8080/thredds/catalog/fmrc/NCEP/RUC2/CONUS_20km/surface/catalog.html";

    JFrame frame = new JFrame("TestTDS");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    main = new JPanel();
    main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

    int n = 10;
    Thread[] task = new Thread[n];
    for (int i = 0; i < n; i++) {
      new TestConcurrentAccess(catName);
      //task[i] = new Thread( new FetchOpendapDataset(dodsName, i));
      task[i] = new Thread( new ReadCatalog(catName, i));
    }

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(40, 300);
    frame.setVisible(true);

    for (int i = 0; i < n; i++)
      task[i].start();

  }

}
