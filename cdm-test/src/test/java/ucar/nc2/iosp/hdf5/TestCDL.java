/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;

import java.io.*;
import java.lang.invoke.MethodHandles;

import ucar.unidata.util.test.TestDir;

/**
 * Compare CDL from NCDump to ncdump -h
 *
 * @author caron
 */
public class TestCDL {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testCDL() throws IOException, InterruptedException {
    File dir = new File(TestDir.cdmUnitTestDir + "formats/netcdf4/files/");
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
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    System.out.println("File "+filename);

    ByteArrayOutputStream bout = new ByteArrayOutputStream(30 * 1000);
    PrintWriter pw = new PrintWriter( new OutputStreamWriter(bout, CDM.utf8Charset));
    NCdumpW.print(ncfile, pw, false, false, false, true, null, null);
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
      return Misc.nearlyEquals(val1, val2);
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
