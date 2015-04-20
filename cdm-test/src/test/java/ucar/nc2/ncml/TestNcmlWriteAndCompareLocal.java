package ucar.nc2.ncml;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * TestWrite NcML, read back and compare with original.
 *
 * This is identical to TestNcmlWriteAndCompareShared, except that we're using local datasets.
 *
 * @author caron
 * @since 11/2/13
 */
@RunWith(Parameterized.class)
public class TestNcmlWriteAndCompareLocal {

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
    List<Object[]> result = new ArrayList<>(500);

    // try everything from these directories
    try {
      addFromScan(result, TestDir.cdmLocalTestDataDir + "point/", new SuffixFileFilter(".ncml"), true);
      addFromScan(result, TestDir.cdmLocalTestDataDir + "ncml/standalone/", new SuffixFileFilter(".ncml"), true);

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

  public TestNcmlWriteAndCompareLocal(String location, boolean compareData) {
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
      org = NetcdfDataset.acquireFile(location, null);

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
        writer.writeXMLexplicit(org, out, null);
      else
        writer.writeXML(org, out, null);
      out.close();

    } catch (IOException ioe) {
      // ioe.printStackTrace();
      assert false : ioe.getMessage();
    }

    // read it back in
    NetcdfFile copy;
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
