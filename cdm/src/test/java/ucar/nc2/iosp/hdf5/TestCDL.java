/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.iosp.hdf5;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestNC2;
import ucar.nc2.NCdump;
import ucar.nc2.util.Misc;

import java.io.*;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestCDL extends TestCase {

  public TestCDL(String name) {
    super(name);
  }

    //////////////////////

  public void testCDL() throws IOException, InterruptedException {
    File dir = new File("C://data/netcdf4/files/");
    File[] files = dir.listFiles();

    for (File f : files) {
      String name = f.getAbsolutePath();
      if (name.endsWith(".nc")) {
        //int pos = name.lastIndexOf(".");
        //String prefix = name.substring(0,pos);
        testCDL(name, false);
      }
    }
  }

  public void testOneCDL() throws IOException, InterruptedException {
    testCDL("C:\\data\\netcdf4\\files\\tst_ncml.nc", true);
  }

  private void testCDL(String filename, boolean show) throws IOException, InterruptedException {
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("File "+filename);

    ByteArrayOutputStream bout = new ByteArrayOutputStream(30 * 1000);
    NCdump.print(ncfile, bout, false, false, false, true, null, null);
    String njCDL = bout.toString();
    if (show) System.out.println("============================================");
    if (show) System.out.println("njCDL " + njCDL);
    if (show) System.out.println("---------------------");

    String cdl = getCDL(filename);
    if (show) System.out.println("CDL " + cdl);
    if (show) System.out.println("---------------------");

    String[] ncTokens = njCDL.split("[\\s,;]+");
    String[] cdlTokens = cdl.split("[\\s,;]+");
    int countNC = 0;
    int countCDL = 0;
    while (countNC < ncTokens.length && countCDL < cdlTokens.length) {

      if (!ncTokens[countNC].equals(cdlTokens[countCDL])) { // tokens match
        if (matchDoubleToken(ncTokens[countNC], cdlTokens[countCDL])) { // numeric match
          if (show) System.out.println("ok double "+ncTokens[countNC]+" == "+cdlTokens[countCDL]);
        } else {
          System.out.println("mismatch "+ncTokens[countNC]+" != "+cdlTokens[countCDL]);
        }
      } else {
        if (show) System.out.println("ok "+ncTokens[countNC]+" == "+cdlTokens[countCDL]);
      }

      countNC++;
      countCDL++;
    }
  }

  private boolean matchDoubleToken(String toke1, String toke2) {
    try {
      double val1 = Double.parseDouble(toke1);
      double val2 = Double.parseDouble(toke2);
      return Misc.closeEnough(val1, val2);
    } catch (NumberFormatException e) {
      //System.out.println(e.getMessage());
      return false;
    }
  }

  private String getCDL(String filename) throws IOException, InterruptedException {

     Runtime run = Runtime.getRuntime();
     Process p = run.exec(new String[] {"ncdump", "-h", filename});
     StreamGobbler err = new StreamGobbler(p.getErrorStream(), "ERR");
     StreamCapture out = new StreamCapture(p.getInputStream());
     err.start();
     out.start();

     // any error???
     int exitVal = p.waitFor();
     if (exitVal != 0)
       throw new IOException("run.exec failed "+filename);

     return out.result.toString();
  }

  class StreamCapture extends Thread {
    InputStream is;
    StringBuilder result = new StringBuilder(10000);

    StreamCapture(InputStream is) {
      this.is = is;
    }

    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null)
          result.append(line).append("\n");
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  ///////////////////////////////////////////////////////


  public void testExec() throws IOException, InterruptedException {

    Runtime run = Runtime.getRuntime();
    //Process p = run.exec(new String[] {"C:\\cdev\\install\\bin\\ncdump.exe", "-h", "C:/data/netcdf4/files/tst_v2.nc"});
    Process p = run.exec(new String[] {"ncdump", "-h", "C:/data/netcdf4/files/tst_v2.nc"});
    //Process p = run.exec(new String[]{"cmd.exe", "/C", "dir"}, null, new File("C:/data/netcdf4/files/"));
    //Process p = run.exec(new String[]{"cmd.exe", "/C", "set"}, null, new File("C:/data/netcdf4/files/"));

    StreamGobbler err = new StreamGobbler(p.getErrorStream(), "ERR");
    StreamGobbler out = new StreamGobbler(p.getInputStream(), "OUT");
    err.start();
    out.start();

    // any error???
    int exitVal = p.waitFor();
    System.out.println("ExitValue: " + exitVal);

    /* InputStream err = new BufferedInputStream( p.getErrorStream());
    //thredds.util.IO.copy(err, System.out);
    System.out.println("---------------------");

    InputStream in = new BufferedInputStream(p.getInputStream());
    thredds.util.IO.copy(in, System.out);

    System.out.println("---------------------");
    int exitVal = p.waitFor();
    System.out.println("Process exitValue: " + exitVal); */

  }

  class StreamGobbler extends Thread {
    InputStream is;
    String type;

    StreamGobbler(InputStream is, String type) {
      this.is = is;
      this.type = type;
    }

    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null)
          System.out.println(type + ">" + line);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  public static void main2(String args[]) {
    String test = "abc  hello;;; ;iou";
    String[] tokes = test.split("[\\s;]+");
    for (String t : tokes)
      System.out.println(" "+t);
    System.out.println("dobe");
  }
}
