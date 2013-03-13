package ucar.nc2.stream;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CompareNetcdf2;

import java.io.*;
import java.util.Formatter;

import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

public class TestNetcdfStream {
  String serverRoot = TestDir.cdmUnitTestDir + "formats";

  @Test
  public void utestProblem() throws IOException {
    doOne(serverRoot+"/netcdf4/vlenBigEndian.nc");
  }

  @Test
  public void testScan() throws IOException {
    /*    */
    scanDir(serverRoot+"/netcdf3/", ".nc");
    scanDir(serverRoot+"/netcdf4/", ".nc");
    /*scanDir(serverRoot+"/netcdf4/", new FileFilter(){
    	public boolean accept(File pathName){
    		return  !pathName.getPath().contains("vlen") && pathName.getPath().endsWith(".nc");
    	}
    });*/    
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
     //scanDir(serverRoot+"/gempak/", ".gem");
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
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
       public int doAct(String filename) throws IOException {
         doOne(filename);
         return 1;
       }
     }, true);
  }

  void doOne(String filename) throws IOException {
    String name = StringUtil2.substitute(filename.substring(serverRoot.length()), "\\", "/");
    String remote = "http://localhost:8081/thredds/cdmremote/testCdmremote" + name;
    System.out.printf("---------------------------\n");
    System.out.printf("TEST %s and %s%n", filename, remote);
    compare(filename, remote);
  }

  void compare(String file, String remote) throws IOException {
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
  public static void main3(String[] args) {
    try {
      String filename = "C:/data/formats/netcdf3/testWrite.nc";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      File file = new File("C:/temp/out.ncs");
      FileOutputStream fos = new FileOutputStream(file);
      writer.streamAll( fos);
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
      writer.streamAll( fos);
      fos.close();

      NetcdfFile ncfileBack = NetcdfFile.open(file.getPath());

      Formatter f = new Formatter();
      CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, true);
      boolean ok = cn.compare(ncfile, ncfileBack);
      if (ok) {
        double ratio = (double) file.length()/fileIn.length();
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
