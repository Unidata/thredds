/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmr;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Compare cdmremote datasets with local, only take files < 100K to keep things from slowing way down.
 *
 * @author caron
 * @since 10/21/2015.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCdmRemoteCompareDataP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static String contentRoot = TestDir.cdmUnitTestDir + "formats";
  static String urlPath = "cdmremote/scanCdmUnitTests/formats";

  static int total, success;

  @AfterClass
  static public void show() {
    System.out.printf("success = %d/%d %n", success, total);
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {

    List<Object[]> result = new ArrayList<>(500);

    addFromScan(result, contentRoot + "/netcdf3/", new SuffixFileFilter(".nc"));
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

    return result;
  }

  static void addFromScan(final List<Object[]> list, String dirName, FileFilter ff) {
    try {
      TestDir.actOnAll(dirName, ff, new TestDir.Act() {
        public int doAct(String filename) throws IOException {
          File file = new File(filename);
          if (file.length() < 100 * 1000) { // 100K
            list.add(new Object[]{filename});
            return 1;
          } else {
            return 0;
          }
        }
      }, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////

  public TestCdmRemoteCompareDataP(String filename) {
    this.filename = filename;
  }

  String filename;

  @Test
  public void doOne() throws IOException {
    String name = StringUtil2.substitute(filename.substring(contentRoot.length()), "\\", "/");
    String remote = TestOnLocalServer.withHttpPath(urlPath + name);
    total++;
    success += compareDatasets(filename, remote, true);
  }

  static int compareDatasets(String local, String remote, boolean readData) throws IOException {
    System.out.printf("--Compare %s to %s%n", local, remote);
    try (NetcdfFile ncfile = NetcdfDataset.openFile(local, null);
         NetcdfFile ncremote = new CdmRemote(remote)) {

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
      boolean ok = mind.compare(ncfile, ncremote, new NcstreamObjFilter(), false, false, readData);
      if (!ok) {
        System.out.printf("  %s%n", f);
      }
      Assert.assertTrue(local + " != " + remote, ok);
    }
    return 1;
  }

  public static class NcstreamObjFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      String name = att.getShortName();
      if (name.equals(_Coordinate.Axes)) return false;
      return true;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      return true;
    }

  }

}

