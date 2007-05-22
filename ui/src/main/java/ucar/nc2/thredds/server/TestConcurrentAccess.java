package ucar.nc2.thredds.server;

import ucar.nc2.ui.StopButton;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.NCdump;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import javax.swing.*;
import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.util.List;

public class TestConcurrentAccess implements Runnable {
  private String name;
  private int who;

  private StopButton stopButton;
  private JLabel label;

  TestConcurrentAccess(String name, int who) throws IOException {
    this.name = name;
    this.who = who;

    JPanel p = new JPanel();
    p.setBorder( BorderFactory.createLineBorder(Color.black ));

    p.add(new JLabel(name+":"));
    label = new JLabel();
    p.add(label);
    stopButton = new StopButton("stop");
    p.add(stopButton);
    main.add( p);
  }


  public void run() {
    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.openDataset(name);
      System.out.println(who+" Open "+name);
      List vars = ncd.getVariables();
      for (int i = 0; i < vars.size(); i++) {
        Variable v = (Variable) vars.get(i);
        System.out.println(" "+who+" "+v.getName());
        read(v);
      }
    } catch (IOException e) {
      e.printStackTrace();
      label.setText("Error");
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
      origin[i] = shape[i]/2;
      shape[i] = 1;
    }
    Array data =  v.read(origin,shape);
    NCdump.printArray(data, who+" "+v.getName(), System.out, null);
  }

  public static JPanel main;
  public static void main(String args[]) throws IOException {
    String name = "http://localhost:8080/thredds/dodsC/fmrc/NAM-CONUS-12/Formica-NAM-CONUS-12_best.ncd";

    JFrame frame = new JFrame("TestTDS");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    main = new JPanel();
    main.setLayout( new BoxLayout(main, BoxLayout.Y_AXIS));

    int n = 10;
    Thread[] task  = new Thread[n];
    for (int i=0; i<n; i++)
      task[i] = new Thread( new TestConcurrentAccess(  name, i));

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(40, 300);
    frame.setVisible(true);

    for (int i=0; i<n; i++)
      task[i].start();

  }

}
