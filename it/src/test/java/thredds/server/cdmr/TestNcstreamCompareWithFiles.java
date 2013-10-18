package thredds.server.cdmr;

import org.junit.Test;
import thredds.TestWithLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.util.CompareNetcdf2;

import java.io.*;
import java.util.Formatter;

import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

public class TestNcstreamCompareWithFiles {
  String contentRoot = TestDir.cdmUnitTestDir + "formats";

  @Test
  public void utestProblem() throws IOException {
    doOne(contentRoot + "/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf");
  }

  int fail = 0;
  int success = 0;

  @Test
  public void testScan() throws IOException {
    scanDir(contentRoot +"/netcdf3/", ".nc");
    scanDir(contentRoot +"/netcdf4/", ".nc");

    scanDir(contentRoot +"/hdf5/",  new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getPath().endsWith(".h5") || pathname.getPath().endsWith(".he5");
      }
    });   // */
    scanDir(contentRoot +"/hdf4/",  new FileFilter() {
       public boolean accept(File pathname) {
         return pathname.getPath().endsWith(".hdf") || pathname.getPath().endsWith(".eos");
       }
     }); 
    /*  */
    scanDir(contentRoot + "/grib1/", new FileFilter() {
      public boolean accept(File pathname) {
        return !pathname.getPath().endsWith(".gbx9") && !pathname.getPath().endsWith(".ncx");
      }
    });
    scanDir(contentRoot + "/grib2/", new FileFilter() {
      public boolean accept(File pathname) {
        return !pathname.getPath().endsWith(".gbx9") && !pathname.getPath().endsWith(".ncx");
      }
    }); //*/
    scanDir(contentRoot +"/gini/", ".gini");
    scanDir(contentRoot +"/gempak/", ".gem");

    System.out.printf("success = %d fail = %d%n", success, fail);
    assert fail == 0 : "failed="+fail;
  }

  void scanDir(String dirName, final String suffix) throws IOException {
    scanDir(dirName, new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getPath().endsWith(suffix);
      }
    });
  }

  void scanDir(String dirName, FileFilter ff) throws IOException {
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        doOne(filename);
        return 1;
      }
    }, true);
  }

  void doOne(String filename) throws IOException {
    String name = StringUtil2.substitute(filename.substring(contentRoot.length()), "\\", "/");
    String remote = TestWithLocalServer.server + "cdmremote/scanCdmUnitTests/formats" + name;
    //System.out.printf("---------------------------\n");
    //System.out.printf("TEST %s and %s%n", filename, remote);
    compareDatasets(filename, remote);
  }

  private void compareDatasets(String local, String remote) throws IOException {
    //System.out.printf("--Compare %s to %s%n", local, remote);
    NetcdfFile ncfile = null, ncremote = null;
    try {
      ncfile = NetcdfDataset.openFile(local, null);
      ncremote = new CdmRemote(remote);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
      boolean ok = mind.compare(ncfile, ncremote, new NcstreamObjFilter(), false, false, false);
      if (!ok) {
        System.out.printf(" NOT OK%n%s%n", f);
        fail++;
      } else {
        success++;
      }
    } finally {
      if (ncfile != null) ncfile.close();
      if (ncremote != null) ncremote.close();
    }
  }

  private class NcstreamObjFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      if (name.equals(_Coordinate.Axes)) return false;

      return true;
    }

  }

  //////////////////////////////////////////////////////////////
  public static void main3(String[] args) {
    try {
      String filename = "C:/data/formats/netcdf3/testWrite.nc";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      File file = new File("C:/temp/out.ncs");
      FileOutputStream fos = new FileOutputStream(file);
      writer.streamAll(fos);
      fos.close();

      NetcdfFile ncfileBack = NetcdfFile.open(file.getPath());

      Formatter f = new Formatter();
      CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, true);
      boolean ok = cn.compare(ncfile, ncfileBack);
      if (ok)
        System.out.printf("compare %s ok %n", file);
      else
        System.out.printf("compare %s NOT OK %n%s", file, f.toString());

      ncfileBack.close();
      ncfile.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main2(String[] args) {
    try {
      String remote = "http://localhost:8081/thredds/cdmremote/testCdmremote/netcdf3/testWrite.nc";
      CdmRemote ncfileRemote = new CdmRemote(remote);

      String fileOut = "C:/temp/out2.ncs";
      ncfileRemote.writeToFile(fileOut);
      NetcdfFile ncfileBack = NetcdfFile.open(fileOut);

      String filename = "C:/data/formats/netcdf3/testWrite.nc";
      NetcdfFile ncfileOrg = NetcdfFile.open(filename);

      Formatter f = new Formatter();
      CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, true);
      boolean ok = cn.compare(ncfileOrg, ncfileBack);
      if (ok)
        System.out.printf("compare %s ok %n", fileOut);
      else
        System.out.printf("compare %s NOT OK %n%s", fileOut, f.toString());

      ncfileBack.close();
      ncfileOrg.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static void write(String filenameIn, String filenameOut) {
    long start = System.currentTimeMillis();
    try {
      File fileIn = new File(filenameIn);
      System.out.printf("COPY %s len = %d%n", filenameIn, fileIn.length());
      NetcdfFile ncfile = NetcdfFile.open(filenameIn);
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      File file = new File(filenameOut);
      FileOutputStream fos = new FileOutputStream(file);
      writer.streamAll(fos);
      fos.close();

      NetcdfFile ncfileBack = NetcdfFile.open(file.getPath());

      Formatter f = new Formatter();
      CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, true);
      boolean ok = cn.compare(ncfile, ncfileBack);
      if (ok) {
        double ratio = (double) file.length() / fileIn.length();
        System.out.printf("compare %s ok len = %d ratio = %f%n", file, file.length(), ratio);
      } else {
        System.out.printf("compare %s NOT OK %n%s", file, f.toString());
      }
      ncfileBack.close();
      ncfile.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf("That took %d msecs%n", took);
  }


  public static void main(String[] args) {
    //write("C:/dev/github/thredds/cdm/src/test/data/testWrite.nc", "C:/tmp/out.ncs");
    write("G:/nomads/cfsr/hpr-mms/pgbf/2008/200812/pgbf2008122500.01.200901.avrg.00Z.grb2", "C:/tmp/out.ncs");
  }

}
