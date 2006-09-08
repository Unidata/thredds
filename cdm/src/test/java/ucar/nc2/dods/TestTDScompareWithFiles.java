package ucar.nc2.dods;

import junit.framework.*;
import java.io.*;
import java.util.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.ma2.*;
import ucar.unidata.util.StringUtil;

/** compare files served hrough netcdf-DODS server. */

public class TestTDScompareWithFiles extends TestCase {
  static boolean showCompare = false, showEach = false, showStringValues = false;

  public TestTDScompareWithFiles( String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    doOne("conventions/zebra/SPOL_3Volumes.nc");
    doOne("conventions/coards/inittest24.QRIDV07200.ncml"); //
    doOne("conventions/atd/rgg.20020411.000000.lel.ll.nc"); //
    doOne("conventions/awips/awips.nc"); //
    doOne("conventions/cf/ipcc/cl_A1.nc"); //
    doOne("conventions/csm/o3monthly.nc"); //
    doOne("conventions/gdv/OceanDJF.nc"); //
    doOne("conventions/gief/coamps.wind_uv.nc"); //
    doOne("conventions/mars/temp_air_01082000.nc"); //
    doOne("conventions/mm5/n040.nc"); //
    doOne("conventions/nuwg/eta.nc"); //
    doOne("conventions/nuwg/ruc.nc"); //
    doOne("conventions/wrf/wrfout_v2_Lambert.nc"); //
    doOne("conventions/mm5/n040.nc"); //

    /* doOne("grib2/ndfd.wmo");
    doOne("grib2/eta2.wmo");
    doOne("image/dmsp/F14200307192230.n.OIS");
    doOne("image/gini/n0r_20041013_1852-u");
    doOne("image/gini/n0r_20041013_1852"); //
    doOne("ldm/grib/AVN_H.wmo"); //
    doOne("AStest/wam/Atl/EPPE_WAM_Atl_200202281500.nc"); // */
  }

  String path = "ncdodsTest";
  String root = "C:/data/ncdodsTest/";

  public void testCompareAll() throws IOException {
    readAllDir(root+"ncml", ".ncml");
  }

  void readAllDir(String dirName, String suffix) throws IOException {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory()) continue;

      String name = f.getAbsolutePath();
      if (!name.endsWith(suffix)) continue;

      doOne(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath(), suffix);
    }

  }


  private void doOne(String filename) throws IOException {
    filename = StringUtil.replace(filename, '\\', "/");
    filename = StringUtil.remove(filename, root);
    String dodsUrl = TestLocalDodsServer.alldata+filename;
    String localPath = root+filename;
    System.out.println("--Compare "+localPath+" to "+dodsUrl);
    compareDatasets(dodsUrl, localPath);
  }

  private void compareDatasets(String dodsUrl, String ncfile) throws IOException {
    NetcdfDataset org_ncfile = NetcdfDataset.openDataset(ncfile);
    NetcdfDataset dods_file = NetcdfDataset.openDataset(dodsUrl);
    compareDatasets( org_ncfile, dods_file);
  }

  private void compareDatasets(NetcdfFile org, NetcdfFile dods) {
    if ((org.getId() != null) || (dods.getId() != null))
      assert org.getId().equals( dods.getId());
    if ((org.getTitle() != null) || (dods.getTitle() != null))
      assert org.getTitle().equals( dods.getTitle());

    compareGroups( org.getRootGroup(), dods.getRootGroup());
  }

  private void compareGroups(Group org, Group dods) {
    if (showCompare) System.out.println("compareGroups  "+org.getName()+" "+dods.getName());
    assert org.getName().equals( dods.getName());

    // dimensions
    checkAll( org.getDimensions(), dods.getDimensions(), true);

    // attributes
    checkAll( org.getAttributes(), dods.getAttributes(), true);

    // variables
    List vars = org.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable orgV =  (Variable) vars.get(i);
      Variable dodsV =  dods.findVariable( orgV.getShortName());
      assert dodsV != null : " cant find "+orgV.getName();

     // if (orgV.getDataType() != DataType.CHAR)
     //   compareVariables(orgV, dodsV);
     // else if (dodsV.getDataType() == DataType.CHAR)
        compareVariables(orgV, dodsV);
     // else
     //   compareStringVariables(orgV, dodsV);
    }

    // nested groups
    List groups = checkAll( org.getGroups(), dods.getGroups(), true);
    for (int i = 0; i < groups.size(); i+=2) {
      Group orgGroup =  (Group) groups.get(i);
      Group dodsGroup =  (Group) groups.get(i+1);
      compareGroups(orgGroup, dodsGroup);
    }
  }

  private void compareStringVariables(Variable org, Variable dods) {
    if (showCompare) System.out.println("compareStringVariables  "+org.getName()+" "+dods.getName());
    assert org.getName().equals( dods.getName());
    assert org.getDataType() == DataType.CHAR;
    assert dods.getDataType() == DataType.STRING;

    // dimensions
    List orgDims = org.getDimensions();
    if (org.getRank() > 0) {
      assert org.getRank()-1 == dods.getRank();
      orgDims = orgDims.subList(0, org.getRank()-1);
    }
    checkAll( orgDims, dods.getDimensions(), false);

    // attributes
    checkAll( org.getAttributes(), dods.getAttributes(), true);

    // values
    try {
      Array orgData = org.read();
      assert orgData instanceof ArrayChar;
      ArrayChar.StringIterator siter = ((ArrayChar)orgData).getStringIterator();

      Array dodsData = dods.read();
      assert dodsData instanceof ArrayObject;
      IndexIterator ii = dodsData.getIndexIterator();
      while (ii.hasNext()) {
        String dodsString = (String) ii.getObjectNext();
        String orgString = siter.next();

        if (showStringValues) System.out.println("org=<"+orgString +"> dods=<"+dodsString+">");
        assert dodsString.equals(orgString) : "<"+orgString +"> != <"+dodsString+">";
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void compareVariables(Variable org, Variable dods) {
    if (showCompare) System.out.println("compareVariables  "+org.getName()+" "+dods.getName());
    assert org.getName().equals( dods.getName());
    assert org.getDataType() == dods.getDataType() : org.getDataType() +" != "+ dods.getDataType();

    // dimensions
    checkAll( org.getDimensions(), dods.getDimensions(), true);

    // attributes
    check1in2( org.getAttributes(), dods.getAttributes());

    List atts = dods.getAttributes();
    for (int i = 0; i < atts.size(); i++) {
      Attribute dodsAtt =  (Attribute) atts.get(i);
      if (dodsAtt.getName().startsWith(_Coordinate.Axes)) continue; // ok

      Attribute orgAtt =  dods.findAttribute( dodsAtt.getName());
      assert orgAtt != null;
      assert dodsAtt.equals( orgAtt);
    }

        // nested variables
    if (org instanceof Structure)  {
      assert (dods instanceof Structure);
      Structure orgS = (Structure) org;
      Structure dodsS = (Structure) dods;

      List vars = checkAll( orgS.getVariables(), dodsS.getVariables(), true);
      for (int i = 0; i < vars.size(); i+=2) {
        Variable orgV =  (Variable) vars.get(i);
        Variable dodsV =  (Variable) vars.get(i+1);
        compareVariables(orgV, dodsV);
      }
    }
  }

   // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paited objects.
  public ArrayList checkAttributes(List list1, List list2, boolean required) {
    ArrayList result = new ArrayList();

    Iterator iter1 = list1.iterator();
    while ( iter1.hasNext()) {
      checkEach(iter1.next(), list1, list2, result, required);
    }

    Iterator iter2 = list2.iterator();
    while ( iter2.hasNext()) {
      checkEach(iter2.next(), list2, list1, null, required);
    }

    return result;
  }

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paited objects.
  public ArrayList checkAll(List list1, List list2, boolean required) {
    ArrayList result = new ArrayList();

    Iterator iter1 = list1.iterator();
    while ( iter1.hasNext()) {
      checkEach(iter1.next(), list1, list2, result, required);
    }

    Iterator iter2 = list2.iterator();
    while ( iter2.hasNext()) {
      checkEach(iter2.next(), list2, list1, null, required);
    }

    return result;
  }

    // make sure each object in  list 1 is in list 2, using equals().
  public void check1in2(List list1, List list2) {

    Iterator iter1 = list1.iterator();
    while ( iter1.hasNext()) {
      checkEach(iter1.next(), list1, list2, null, true);
    }
  }


  public void checkEach(Object want1, List list1, List list2, List result, boolean required) {
    int index2 = list2.indexOf( want1);
    if (required) {
      assert (index2 >= 0) : grabErr(want1 + " not in list 2");
  } else if (index2 < 0) {
      System.out.println("WARN: "+want1 + " not in list 2");
      return;
    }

    Object want2 = list2.get( index2);

    int index1 = list1.indexOf( want2);
    if (required) {
      assert (index1 >= 0) :
              want2 + " not in list 1";
    } else if (index1 < 0) {
      System.out.println("WARN: "+want2 + " not in list 1");
      return;
  }

    Object want = list1.get( index1);
    assert want == want1: want1 + " not == "+ want;

    if (showEach) System.out.println("  OK <"+want1+"> equals <"+want2+">");
    if (result != null) {
      result.add(want1);
      result.add(want2);
    }
  }

  private String grabErr (String s) {
    return "ERR= "+s;
  }

  public static void main(String[] args) throws IOException {
    TestTDScompareWithFiles t = new TestTDScompareWithFiles("dummy");
    //t.testCompare();
  }
}
