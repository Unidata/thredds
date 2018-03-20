/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLWriter;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 4, 2008
 */
public class TimingTestRunner {

  static private class Average {
    private ArrayList values = new ArrayList();

    public void add(double value) {
      values.add(new Double(value));
    }

    public double mean() {
      int elements = values.size();
      if (elements == 0) throw new IllegalStateException("No values");
      double sum = 0;
      for (int i = 0; i < values.size(); i++) {
        Double valo = (Double) values.get(i);
        sum += valo.doubleValue();
      }
      return sum / elements;
    }

    public double stddev() {
      double mean = mean();
      double stddevtotal = 0;
      for (int i = 0; i < values.size(); i++) {
        Double valo = (Double) values.get(i);
        double dev = valo.doubleValue() - mean;
        stddevtotal += dev * dev;
      }

      return Math.sqrt(stddevtotal / values.size());
    }

    public String toString() {
      return " avg= " + mean() + " stdev= " + stddev() + " count= " + values.size();
    }
  }

  interface MClosure {
    void run(String filename) throws IOException, InvalidRangeException;
  }

  static void testAllInDir(File dir, MClosure closure) throws IOException, InvalidRangeException {
    File[] fa = dir.listFiles();
    if (fa == null || fa.length == 0) return;

    List<File> list = Arrays.asList(fa);
    Collections.sort(list);

    for (File f : list) {
      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        closure.run(f.getPath());
      }
    }
  }

  public void testWriteNcml() throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    final NcMLWriter writer = new NcMLWriter();

    testAllInDir(new File("C:/data/grib/"), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith("grib1")) return;
        NetcdfFile ncfile = NetcdfDataset.openFile(filename, null);
        File fileout = new File(filename + ".ncml");
        if (fileout.exists()) fileout.delete();
        ncfile.writeNcML(new FileOutputStream(fileout), filename);
        System.out.println(" wrote ncml file  =" + fileout);

      }
    });
  }

  public void testOpenFile() throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir(new File("C:/data/grib/"), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith("ncml")) return;
        System.out.println(" open ncml file  =" + filename);
        openFile(filename, fileAvg, true);
      }
    });
    System.out.println(" open ncml file  =" + fileAvg);
  }

  static void openFile(String filename, Average avg, boolean enhance) throws IOException, InvalidRangeException {
    try {
      long start = System.nanoTime();
      NetcdfFile ncfile = enhance ? NetcdfDataset.openDataset(filename) : NetcdfDataset.openFile(filename, null);
      long end = System.nanoTime();
      double took = (double) ((end - start)) / 1000 / 1000 / 1000;
      ncfile.close();
      if (avg != null) avg.add(took);
    } catch (Exception e) {
      System.out.println("BAD " + filename);
      e.printStackTrace();
    }
  }

  // testing on remote machines like motherlode

  static void testOpenFile(String dir, final String suffix) throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir(new File(dir), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith(suffix)) return;
        //System.out.println(" open "+suffix+" file  ="+filename);
        openFile(filename, fileAvg, false);
      }
    });
    System.out.println("*** open " + suffix + " files  =" + fileAvg);
  }

  static void testOpenDataset(String dir, final String suffix) throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir(new File(dir), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith(suffix)) return;
        //System.out.println(" open "+suffix+" file  ="+filename);
        openFile(filename, fileAvg, true);
      }
    });
    System.out.println("*** open " + suffix + " datasets  =" + fileAvg);
  }

  public static void main(String args[]) throws IOException, InvalidRangeException {
    String dir = args[0];
    String suffix = args[1];
    testOpenFile(dir, suffix);
    testOpenDataset(dir, suffix);
  }


}
