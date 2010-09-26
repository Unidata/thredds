package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.TestAll;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.nio.channels.WritableByteChannel;
import java.util.Formatter;

import junit.framework.TestCase;


public class TestNetcdfStream extends TestCase {
  String serverRoot = "E:/formats";
  
  public TestNetcdfStream(String name) {
    super(name);
  }

  public void testProblem() throws IOException {
    doOne("C:/data/formats/netcdf4/tst_enums.nc");
  }

  public void testScan() throws IOException {
    /*    */
    scanDir(serverRoot+"/netcdf3/", ".nc");
    scanDir(serverRoot+"/netcdf4/", ".nc");
    scanDir(serverRoot+"/hdf5/",  new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getPath().endsWith(".h5") || pathname.getPath().endsWith(".he5");
      }
    });   // */
    scanDir(serverRoot+"/hdf4/",  new FileFilter() {
       public boolean accept(File pathname) {
         return pathname.getPath().endsWith(".hdf") || pathname.getPath().endsWith(".eos");
       }
     }); 
    /*  */
    scanDir(serverRoot+"/grib/",  new FileFilter() {
       public boolean accept(File pathname) {
         return pathname.getPath().endsWith(".grib") || pathname.getPath().endsWith(".grib1") || pathname.getPath().endsWith(".grib2");
       }
     });
     scanDir(serverRoot+"/gini/", ".gini");
     scanDir(serverRoot+"/gempak/", ".gem");
     scanDir(serverRoot+"/gempak/", ".gem");
     scanDir(serverRoot+"/gnexrad2empak/", ".ar2v"); // */
  }

  void scanDir(String dirName, final String suffix) throws IOException {
    scanDir(dirName, new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getPath().endsWith(suffix);
      }
    });
  }

  void scanDir(String dirName, FileFilter ff) throws IOException {
     TestAll.actOnAll( dirName, ff, new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        doOne(filename);
        return 1;
      }
    }, true);
  }

  void doOne(String filename) throws IOException {
    String name = StringUtil.substitute(filename.substring(serverRoot.length()), "\\", "/");
    String remote = "http://localhost:8080/thredds/cdmremote/testCdmremote" + name;
    System.out.printf("%s%n", filename);
    compare(filename, remote);
  }

  void compare(String file, String remote) throws IOException {
    System.out.printf("---------------------------\n");
    NetcdfFile ncfile = NetcdfDataset.openFile(file, null);
    NetcdfFile ncfileRemote = new CdmRemote(remote);

    Formatter f= new Formatter();
    CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, false);
    boolean ok = cn.compare(ncfile, ncfileRemote);
    if (ok)
      System.out.printf("compare %s ok %n", file);
    else
      System.out.printf("compare %s NOT OK %n%s", file, f.toString());
    ncfile.close();
    ncfileRemote.close();
  }

  //////////////////////////////////////////////////////////////
  public static void main2(String[] args) {
    try {
      String filename = "C:/data/formats/netcdf3/testWrite.nc";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      File file = new File("C:/temp/out.ncs");
      FileOutputStream fos = new FileOutputStream(file);
      WritableByteChannel wbc = fos.getChannel();
      writer.streamAll( wbc);
      wbc.close();

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

  public static void main(String[] args) {
    try {
      String remote = "http://localhost:8080/thredds/cdmremote/testCdmremote/netcdf3/testWrite.nc";
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
}
