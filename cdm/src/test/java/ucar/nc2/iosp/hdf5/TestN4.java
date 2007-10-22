package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.*;
import ucar.ma2.Section;
import ucar.ma2.Array;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.List;

/**
 * Test nc2 read JUnit framework.
 */

public class TestN4 extends TestCase {

  public TestN4(String name) {
    super(name);
  }

  public void testOpen() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    List<Variable> vars = ncfile.getVariables();
    Collections.sort(vars);
    for (Variable v : vars) System.out.println(" "+v.getName());
    System.out.println("nvars = "+ncfile.getVariables().size());
    ncfile.close();
  }

  public void testReadOne() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    TestH5read.readAllData(filename);
  }

  public void test() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    Variable v = ncfile.findVariable("c213");
    Array data = v.read();
  }

  public void testEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_enum_data.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    Variable v = ncfile.findVariable("primary_cloud");
    Array data = v.read();
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    NCdump.printArray(data, "primary_cloud", System.out, null);
    ncfile.close();
  }

  public void testVlenStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_strings.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("measure_for_measure_var");
    Array data = v.read();
    NCdump.printArray(data, "measure_for_measure_var", System.out, null);
    ncfile.close();
  }

  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/cdm_sea_soundings.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("fun_soundings");
    Array data = v.read();
    NCdump.printArray(data, "fun_soundings", System.out, null);
    ncfile.close();
  }

  public void testStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("cr");
    Array data = v.read();
    NCdump.printArray(data, "cr", System.out, null);
    ncfile.close();
  }

  public void testReadAll() {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
    readAllDir("C:/data/netcdf4/files/");
  }

  public void readAllDir(String dirName) {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"))
        TestH5read.readAllData(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  //////////////////////

  public void testCDL() throws IOException, InterruptedException {
    File dir = new File("C:/data/netcdf4/files/");
    File[] files = dir.listFiles();

    for (File f : files) {
      String name = f.getAbsolutePath();
      if (name.endsWith(".cdl")) {
        int pos = name.lastIndexOf(".");
        String prefix = name.substring(0,pos);
        testCDL(prefix, false);
      }
    }
  }

  public void testOneCDL() throws IOException, InterruptedException {
    testCDL("C:/data/netcdf4/files/c0", true);
  }

  private void testCDL(String prefix, boolean show) throws IOException {
    NetcdfFile ncfile = TestNC2.open(prefix+".nc");
    System.out.println("File "+prefix);

    ByteArrayOutputStream bout = new ByteArrayOutputStream(30 * 1000);
    NCdump.print(ncfile, bout, false, false, false, true, null, null);
    String njCDL = bout.toString();
    if (show) System.out.println("============================================");
    if (show) System.out.println("njCDL " + njCDL);
    if (show) System.out.println("---------------------");

    String cdl = thredds.util.IO.readFile(prefix+".cdl");
    if (show) System.out.println("CDL " + cdl);
    if (show) System.out.println("---------------------");

    String[] ncTokens = njCDL.split("[\\s;]");
    String[] cdlTokens = cdl.split("[\\s;]");
    int countNC = 0;
    int countCDL = 0;
    while (countNC < ncTokens.length && countCDL < cdlTokens.length) {
      while (ncTokens[countNC].length() == 0) countNC++;
      while (cdlTokens[countCDL].length() == 0) countCDL++;

      if (!ncTokens[countNC].equals(cdlTokens[countCDL])) {
        System.out.println("mismatch "+ncTokens[countNC]+" != "+cdlTokens[countCDL]);
        //break;
      }
      else if (show) System.out.println("ok "+ncTokens[countNC]+" == "+cdlTokens[countCDL]);
      countNC++;
      countCDL++;
    }
  }

  private void ncdump(String filename) throws IOException, InterruptedException {

    Runtime run = Runtime.getRuntime();
    Process p = run.exec(new String[] {"C:\\cdev\\install\\bin\\ncdump.exe", "-h", "C:/data/netcdf4/files/tst_v2.nc"});
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

}
