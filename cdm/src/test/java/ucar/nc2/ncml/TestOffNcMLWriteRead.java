/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ncml;

import junit.framework.*;

import ucar.nc2.*;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TestDataset;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.*;

/** test wrinting to NcML, then reading back and comparing to original.
 * Do not use addCoords.
 * */

public class TestOffNcMLWriteRead extends TestCase {
  static boolean showFiles = true, showCompare = false, showEach = false;

  public TestOffNcMLWriteRead( String name) {
    super(name);
  }

  private ArrayList<String> files;
  private String datadir = TestAll.testdataDir + "grid/netcdf/";

  public void setUp() {
    files = new ArrayList<String>();

    files.add( datadir+"atd-radar/rgg.20020411.000000.lel.ll.nc");
    files.add( datadir+"awips/19981109_1200.nc");
    files.add( datadir+"cf/ccsm2.nc"); //
    files.add( datadir+"coards/cldc.mean.nc");
    files.add( datadir+"csm/o3monthly.nc");
    files.add( datadir+"gdv/OceanDJF.nc");
    files.add( datadir+"gief/coamps.wind_uv.nc");
    files.add( datadir+"mars/temp_air_01082000.nc");
    files.add( datadir+"nuwg/eta.nc");
    files.add( datadir+"nuwg/ocean.nc");
    files.add( datadir+"wrf/wrfout_v2_Lambert.nc");
    files.add( datadir+"atd-radar/SPOL_3Volumes.nc"); // */
  }

  public void testReadAsNcfile() throws Exception {
    for (String s : files)
      convertAsNcfile(s, false);
  }

  public void testReadAsNcfileExplicit() throws Exception {
    for (String s : files)
      convertAsNcfileExplicit(s, false);
  }

  public void testReadAsNcfileWithRecords() throws Exception {
    for (String s : files) {
      convertAsNcfile(s, true);
    }
  }

  public void testReadAsNcdataset() throws Exception {
    for (String s : files) {
      convertAsNcdataset(s, false);
    }
  }

  public void testReadAsNcdatasetWithRecords() throws Exception {
    for (String s : files) {
      convertAsNcdataset(s, true);
    }
  }

  public void utestOne() throws Exception  {
    convertAsNcdataset( "C:/data/conventions/nuwg/ocean.nc", false);
  }

  void readAllDir(String dirName) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (File allFile : allFiles) {
      String name = allFile.getAbsolutePath();
      convertAsNcfile(name, false);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        readAllDir(f.getAbsolutePath());
    }

  }

  //public void testConvert() throws IOException {
    //convert( "C:/data/conventions/gdv/testGDV.nc");
    //convert( "C:/data/conventions/coards/cldc.mean.nc");
  //}

  private void convertAsNcfile(String location, boolean useRecords) throws IOException {
    location = StringUtil.replace(location, '\\', "/");

    if (showFiles) {
     System.out.println("-----------");
     System.out.println(" input filename= "+location);
    }

    NetcdfFile org_ncd = NetcdfDataset.acquireFile(location, null);
    if (useRecords)
      org_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    NcMLWriter writer = new NcMLWriter();

    // make sure writeDirs exists
    File writeDir = new File(TestDataset.writeDir);
    writeDir.mkdirs();

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filename = location.substring(pos+1);
    String ncmlOut = TestDataset.writeDir+filename+ ".ncml";
    if (showFiles) System.out.println(" output filename= "+ncmlOut);
    try {
     OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlOut, false));
     writer.writeXML( org_ncd, out, null);
     out.close();
    } catch (IOException ioe) {
     ioe.printStackTrace();
     assert false;
    }

    // read it back in
    NetcdfFile new_ncd = NetcdfDataset.acquireFile(ncmlOut, null);
    if (useRecords)
      new_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    CompareNetcdf.compareFiles( org_ncd, new_ncd);
    //assert cat.equals( catV1);

    org_ncd.close();
    new_ncd.close();
  }

  private void convertAsNcfileExplicit(String location, boolean useRecords) throws IOException {
    location = StringUtil.replace(location, '\\', "/");

    if (showFiles) {
     System.out.println("-----------");
     System.out.println(" input filename= "+location);
    }

    NetcdfFile org_ncd = NetcdfDataset.acquireFile(location, null);
    if (useRecords)
      org_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    NcMLWriter writer = new NcMLWriter();

    // make sure writeDirs exists
    File writeDir = new File(TestDataset.writeDir);
    writeDir.mkdirs();

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filename = location.substring(pos+1);
    String ncmlOut = TestDataset.writeDir+filename+ ".ncml";
    if (showFiles) System.out.println(" output filename= "+ncmlOut);
    try {
     OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlOut, false));
     writer.writeXMLexplicit( org_ncd, out, null);
     out.close();
    } catch (IOException ioe) {
     ioe.printStackTrace();
     assert false;
    }

    // read it back in
    NetcdfFile new_ncd = NetcdfDataset.acquireFile(ncmlOut, null);
    if (useRecords)
      new_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    CompareNetcdf.compareFiles( org_ncd, new_ncd);
    //assert cat.equals( catV1);

    org_ncd.close();
    new_ncd.close();
  }


  private void convertAsNcdataset(String location, boolean useRecords) throws IOException {
    location = StringUtil.replace(location, '\\', "/");

    NetcdfDataset org_ncd = NetcdfDataset.openDataset(location, false, null);
    if (useRecords)
      org_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    NcMLWriter writer = new NcMLWriter();
    if (showFiles) {
      System.out.println("-----------");
      System.out.println("DS input filename= "+location);
    }

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filename = location.substring(pos+1);
    String ncmlOut = TestDataset.writeDir+filename+ ".ncml";
    if (showFiles) System.out.println(" output filename= "+ncmlOut);
    try {
      OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlOut, false));
      writer.writeXML( org_ncd, out, null);
      out.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert false;
    }

    // read it back in
    NetcdfDataset new_ncd = NetcdfDataset.openDataset(ncmlOut, false, null);
    if (useRecords)
      new_ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    CompareNetcdf.compareFiles( org_ncd, new_ncd);
    //assert cat.equals( catV1);

    org_ncd.close();
    new_ncd.close();
  }

  /* cant do a hashCode compare

  static public void compareDatasets(NetcdfFile org, NetcdfFile ncml) {
    if ((org.getId() != null) || (ncml.getId() != null))
      assert org.getId().equals( ncml.getId());
    if ((org.getTitle() != null) || (ncml.getTitle() != null))
      assert org.getTitle().equals( ncml.getTitle());

    // assert org.getLocation().equals( ncml.getLocation());

    compareGroups( org.getRootGroup(), ncml.getRootGroup());
  }

  static private void compareGroups(Group org, Group ncml) {
    if (showCompare) System.out.println("compareGroups  "+org.getName()+" "+ncml.getName());
    assert org.getName().equals( ncml.getName());

    // dimensions
    checkAll( org.getDimensions(), ncml.getDimensions());

    // attributes
    checkAll( org.getAttributes(), ncml.getAttributes());

    // variables
    List vars = checkAll( org.getVariables(), ncml.getVariables());
    for (int i = 0; i < vars.size(); i+=2) {
      Variable orgV =  (Variable) vars.get(i);
      Variable ncmlV =  (Variable) vars.get(i+1);
      compareVariables(orgV, ncmlV);
    }

    // nested groups
    List groups = checkAll( org.getGroups(), ncml.getGroups());
    for (int i = 0; i < groups.size(); i+=2) {
      Group orgGroup =  (Group) groups.get(i);
      Group ncmlGroup =  (Group) groups.get(i+1);
      compareGroups(orgGroup, ncmlGroup);
    }

  }

  static void compareVariables(Variable org, Variable ncml) {
    if (showCompare) System.out.println("compareVariables  "+org.getName()+" "+ncml.getName());
    assert org.getName().equals( ncml.getName());

    // dimensions
    checkAll( org.getDimensions(), ncml.getDimensions());

    // attributes
    checkAll( org.getAttributes(), ncml.getAttributes());

    /* data !!
    try {
      compareData(org, ncml);
    } catch (IOException e) {
      assert false;
    }

    // nested variables
    if (org instanceof Structure)  {
      assert (ncml instanceof Structure);
      Structure orgS = (Structure) org;
      Structure ncmlS = (Structure) ncml;

      List vars = checkAll( orgS.getVariables(), ncmlS.getVariables());
      for (int i = 0; i < vars.size(); i+=2) {
        Variable orgV =  (Variable) vars.get(i);
        Variable ncmlV =  (Variable) vars.get(i+1);
        compareVariables(orgV, ncmlV);
      }
    }

  }

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paited objects.
  static public ArrayList checkAll(List list1, List list2) {
    ArrayList result = new ArrayList();

    Iterator iter1 = list1.iterator();
    while ( iter1.hasNext()) {
      checkEach(iter1.next(), list1, list2, result);
    }

    Iterator iter2 = list2.iterator();
    while ( iter2.hasNext()) {
      checkEach(iter2.next(), list2, list1, null);
    }

    return result;
  }

  static public void checkEach(Object want1, List list1, List list2, List result) {
    int index2 = list2.indexOf( want1);
    assert (index2 >= 0) : want1 + " not in list 2";
    Object want2 = list2.get( index2);

    int index1 = list1.indexOf( want2);
    assert (index1 >= 0) : want2 + " not in list 1";
    Object want = list1.get( index1);
    if (want != want1)
      System.out.println("why");
    assert want == want1: want1 + " not == "+ want;

    if (showEach) System.out.println("  OK <"+want1+". equals <"+want2+">");
    if (result != null) {
      result.add(want1);
      result.add(want2);
    }
  }

  static void compareData(Variable orgVar, Variable ncmlVar) throws IOException {
    if (showCompare) System.out.println("compareArrays  "+orgVar.getName()+" "+ncmlVar.getName());
    Array org = orgVar.read();
    Array ncml = ncmlVar.read();
    assert org.getSize() == ncml.getSize();
    assert org.getElementType() == ncml.getElementType();

    IndexIterator iterOrg = org.getIndexIterator();
    IndexIterator iterNcml = ncml.getIndexIterator();
    while (iterOrg.hasNext()) {
      if (orgVar.getDataType() == DataType.DOUBLE) {
        double v1 = iterNcml.getDoubleNext();
        double v2 = iterOrg.getDoubleNext();
        if (!Double.isNaN(v1) || !Double.isNaN(v2))
          assert v1 == v2 : v1 + " != "+ v2;
      } else if (orgVar.getDataType() == DataType.FLOAT) {
        float v1 = iterNcml.getFloatNext();
        float v2 = iterOrg.getFloatNext();
        if (!Float.isNaN(v1) || !Float.isNaN(v2))
          assert v1 == v2 : v1 + " != "+ v2;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    TestCompareNcmlWriter t = new TestCompareNcmlWriter("dummy");
  } */
}
