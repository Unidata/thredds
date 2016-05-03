package thredds.server.cdmr;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNcstreamCompareWithFiles {
  static String contentRoot = TestDir.cdmUnitTestDir + "formats";
  static String urlPath = "cdmremote/scanCdmUnitTests/formats";

  static int total, success;

  @AfterClass
  static public void show() {
    System.out.printf("success = %d/%d %n", success, total);
  }

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {

   List<Object[]>  result = new ArrayList<>(500);

    try {
      addFromScan(result, contentRoot +"/netcdf3/", new SuffixFileFilter(".nc"));
      addFromScan(result, contentRoot + "/netcdf4/", new SuffixFileFilter(".nc"));

      addFromScan(result, contentRoot + "/hdf5/", new FileFilter() {
        public boolean accept(File pathname) {
          return pathname.getPath().endsWith(".h5") || pathname.getPath().endsWith(".he5");
        }
      });
      addFromScan(result, contentRoot + "/hdf4/", new FileFilter() {
        public boolean accept(File pathname) {
          return pathname.getPath().endsWith(".hdf") || pathname.getPath().endsWith(".eos");
        }
      });
      addFromScan(result, contentRoot + "/grib1/", new FileFilter() {
        public boolean accept(File pathname) {
          return !pathname.getPath().endsWith(".gbx9") && !pathname.getPath().endsWith(".ncx") && !pathname.getPath().endsWith(".ncx2")&& !pathname.getPath().endsWith(".ncx3");
        }
      });
      addFromScan(result, contentRoot + "/grib2/", new FileFilter() {
        public boolean accept(File pathname) {
          return !pathname.getPath().endsWith(".gbx9") && !pathname.getPath().endsWith(".ncx") && !pathname.getPath().endsWith(".ncx2")&& !pathname.getPath().endsWith(".ncx3");
        }
      });
      addFromScan(result, contentRoot + "/gini/", new SuffixFileFilter(".gini"));
      addFromScan(result, contentRoot + "/gempak/", new SuffixFileFilter(".gem"));

    } catch (IOException e) {
       e.printStackTrace();
     }

    return result;
  }

  static void addFromScan(final List<Object[]> list, String dirName, FileFilter ff) throws IOException {
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        list.add (new Object[] {filename});
        return 1;
      }
    }, true);
  }

  /////////////////////////////////////////////////////////////

  public TestNcstreamCompareWithFiles(String filename) {
    this.filename = filename;
  }

  String filename;

  @Test
  public void doOne() throws IOException {
    String name = StringUtil2.substitute(filename.substring(contentRoot.length()), "\\", "/");
    String remote = TestWithLocalServer.withPath(urlPath + name);
    total++;
    success += compareDatasets(filename, remote);
  }

  static int compareDatasets(String local, String remote) throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openFile(local, null);
         NetcdfFile  ncremote = new CdmRemote(remote)) {

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
      boolean ok = mind.compare(ncfile, ncremote, new NcstreamObjFilter(), false, false, false);
      if (!ok) {
        System.out.printf("--Compare %s to %s%n", local, remote);
        System.out.printf("  %s%n", f);
      }
      Assert.assertTrue(local + " != " + remote, ok);
    }
    return 1;
  }

  private static class NcstreamObjFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      if (name.equals(_Coordinate.Axes)) return false;

      return true;
    }

    @Override public boolean varDataTypeCheckOk(Variable v) { return true; }

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
