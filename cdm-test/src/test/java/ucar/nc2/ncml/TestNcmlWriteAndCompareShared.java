package ucar.nc2.ncml;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.netcdf4.Nc4;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * TestWrite NcML, read back and compare with original.
 *
 * This is identical to TestNcmlWriteAndCompareLocal, except that we're using shared datasets.
 *
 * @author caron
 * @since 11/2/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNcmlWriteAndCompareShared {

  @Before
  public void setLibrary() {
    Nc4Iosp.setLibraryAndPath("/opt/netcdf/lib", "netcdf");
    //Nc4Iosp.setLibraryAndPath("C:/cdev/lib", "netcdf");
    System.out.printf("Nc4Iosp.isClibraryPresent = %s%n", Nc4Iosp.isClibraryPresent());

    // make sure writeDirs exists
    File writeDir = new File(TestDir.temporaryLocalDataDir);
    writeDir.mkdirs();
  }

  @Parameterized.Parameters
  public static List<Object[]> getTestParameters() {
    String datadir = TestDir.cdmUnitTestDir;

    List<Object[]> result = new ArrayList<>(500);

    //result.add(new Object[]{datadir + "formats/netcdf4/tst/test_enum_type.nc", false});
    result.add(new Object[]{datadir + "conventions/atd-radar/rgg.20020411.000000.lel.ll.nc", false});
    result.add(new Object[]{datadir + "conventions/atd-radar/SPOL_3Volumes.nc", false});
    result.add(new Object[]{datadir + "conventions/awips/19981109_1200.nc", false});
    result.add(new Object[]{datadir + "conventions/cf/ccsm2.nc", false}); //
    result.add(new Object[]{datadir + "conventions/coards/cldc.mean.nc", false});
    result.add(new Object[]{datadir + "conventions/csm/o3monthly.nc", false});
    result.add(new Object[]{datadir + "conventions/gdv/OceanDJF.nc", false});
    result.add(new Object[]{datadir + "conventions/gief/coamps.wind_uv.nc", false});
    result.add(new Object[]{datadir + "conventions/mars/temp_air_01082000.nc", true});
    result.add(new Object[]{datadir + "conventions/nuwg/eta.nc", false});
    result.add(new Object[]{datadir + "conventions/nuwg/ocean.nc", true});
    result.add(new Object[]{datadir + "conventions/wrf/wrfout_v2_Lambert.nc", false});

    result.add(new Object[]{datadir +  "formats/grib2/eta2.wmo", false}); //
    result.add(new Object[]{datadir +  "formats/grib2/ndfd.wmo", false}); //

    result.add(new Object[]{datadir +  "formats/gini/n0r_20041013_1852-compress", false}); //
    result.add(new Object[]{datadir +  "formats/gini/ntp_20041206_2154", true}); //
    result.add(new Object[]{datadir +  "formats/dmsp/F14200307192230.n.OIS", false}); //

    result.add(new Object[]{datadir +  "formats/nexrad/level2/6500KHGX20000610_000110", false});
    result.add(new Object[]{datadir +  "formats/nexrad/level2/Level2_KYUX_20060527_2335.ar2v", true});

    // try everything from these directories
      try {
        addFromScan(result, TestDir.cdmUnitTestDir + "formats/netcdf4/", new NotFileFilter( new SuffixFileFilter(".cdl")), false);
      } catch (IOException e) {
        e.printStackTrace();
      }   // */

    return result;
  }

  // FIXME: This method sucks: it doesn't fail when dirName can't be read.
  static void addFromScan(final List<Object[]> list, String dirName, FileFilter ff, final boolean compareData) throws IOException {
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        list.add(new Object[]{filename, compareData});
        return 1;
      }
    }, true);
  }

  /////////////////////////////////////////////////////////////
  boolean showFiles = true;
  boolean compareData = false;

  public TestNcmlWriteAndCompareShared(String location, boolean compareData) {
    this.location = StringUtil2.replace(location, '\\', "/");
    this.compareData = compareData;
  }

  String location;

  int fail = 0;
  int success = 0;

  @Test
  public void compareNcML() throws IOException {
    compareNcML(true, true, true);
    compareNcML(true, false, false);
    compareNcML(false, true, false);
    compareNcML(false, false, true);
    compareNcML(false, false, false);
  }

  public void compareNcML(boolean useRecords, boolean explicit, boolean openDataset) throws IOException {
    if (compareData) useRecords = false;

    if (showFiles) {
      System.out.println("-----------");
      System.out.println("  input filename= " + location);
    }

    NetcdfFile org;
    if (openDataset)
      org = NetcdfDataset.openDataset(location, false, null);
    else
      org  = NetcdfDataset.acquireFile(location, null);

    if (useRecords)
      org.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    NcMLWriter writer = new NcMLWriter();

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filenameTmp = location.substring(pos + 1);
    String ncmlOut = TestDir.temporaryLocalDataDir + filenameTmp + ".ncml";
    if (showFiles) System.out.println(" output filename= " + ncmlOut);
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(ncmlOut, false));
      if (explicit)
        writer.writeXMLexplicit( org, out, null);
      else
        writer.writeXML(org, out, null);
      out.close();

    } catch (IOException ioe) {
      // ioe.printStackTrace();
      assert false : ioe.getMessage();
    }

    // read it back in
    NetcdfFile copy ;
    if (openDataset)
      copy = NetcdfDataset.openDataset(ncmlOut, false, null);
    else
      copy = NetcdfDataset.acquireFile(ncmlOut, null);

    if (useRecords)
      copy.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    try {
      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, compareData);
      boolean ok = mind.compare(org, copy, new Netcdf4ObjectFilter(), false, false, compareData);
      if (!ok) {
        fail++;
        System.out.printf("--Compare %s, useRecords=%s explicit=%s openDataset=%s compareData=%s %n", location, useRecords, explicit, openDataset, compareData);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK (useRecords=%s explicit=%s openDataset=%s compareData=%s)%n", location, useRecords, explicit, openDataset, compareData);
        success++;
      }
      Assert.assertTrue(location, ok);
    } finally {
      org.close();
      copy.close();
    }
  }

  public static class Netcdf4ObjectFilter implements CompareNetcdf2.ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      // added by cdm
      if (name.equals(CDM.CHUNK_SIZES)) return false;
      if (name.equals(CDM.FILL_VALUE)) return false;
      if (name.equals("_lastModified")) return false;

      // hidden by nc4
      if (name.equals(Nc4.NETCDF4_DIMID)) return false;  // preserve the order of the dimensions
      if (name.equals(Nc4.NETCDF4_COORDINATES)) return false;  // ??
      if (name.equals(Nc4.NETCDF4_STRICT)) return false;

      // not implemented yet
      //if (att.getDataType().isEnum()) return false;

      return true;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      if (v.getDataType() == DataType.CHAR) return false;    // temp workaround
      return v.getDataType() != DataType.STRING;
    }
  }
}
