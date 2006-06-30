package ucar.nc2.ncml;

import junit.framework.*;
import java.io.*;
import java.util.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TestDataset;
import ucar.nc2.ncml.NcMLWriter;
import ucar.unidata.util.StringUtil;

/** test wrinting to NcML, then reading back and comparing to original. */

public class TestCompareNcmlWriter extends TestCase {
  static boolean showCompare = false, showEach = false;

  public TestCompareNcmlWriter( String name) {
    super(name);
  }

  public ArrayList files;

  public void setUp() {
    files = new ArrayList();

    files.add( "C:/data/conventions/atd/rgg.20020411.000000.lel.ll.nc");
    files.add( "C:/data/conventions/awips/19981109_1200.nc");
    files.add( "C:/data/conventions/cf/ccsm2.nc");
    files.add( "C:/data/conventions/coards/cldc.mean.nc");
    files.add( "C:/data/conventions/csm/o3monthly.nc");
    files.add( "C:/data/conventions/gdv/OceanDJF.nc");
    files.add( "C:/data/conventions/gief/coamps.wind_uv.nc");
    files.add( "C:/data/conventions/mars/temp_air_01082000.nc");
    files.add( "C:/data/conventions/nuwg/eta.nc");
    files.add( "C:/data/conventions/nuwg/ocean.nc");
    files.add( "C:/data/conventions/wrf/wrfout_v2_Lambert.nc");
    files.add( "C:/data/conventions/zebra/SPOL_3Volumes.nc");
  }

  public void testReadAsNcfile() throws Exception {
    for (int i = 0; i < files.size(); i++) {
      String s = (String) files.get(i);
      convertAsNcfile(s, false);
    }
  }

  public void testReadAsNcfileWithRecords() throws Exception {
    for (int i = 0; i < files.size(); i++) {
      String s = (String) files.get(i);
      convertAsNcfile(s, true);
    }
  }

  public void testReadAsNcdataset() throws Exception {
    for (int i = 0; i < files.size(); i++) {
      String s = (String) files.get(i);
      convertAsNcdataset(s, false);
    }
  }

  public void testReadAsNcdatasetWithRecords() throws Exception {
    for (int i = 0; i < files.size(); i++) {
      String s = (String) files.get(i);
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

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      convertAsNcfile(name, false);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  //public void testConvert() throws IOException {
    //convert( "C:/data/conventions/gdv/testGDV.nc");
    //convert( "C:/data/conventions/coards/cldc.mean.nc");
  //}

  private void convertAsNcfile(String location, boolean useRecords) throws IOException {
     location = StringUtil.replace(location, '\\', "/");

     System.out.println("-----------");
     System.out.println(" input filename= "+location);

     NetcdfFile org_ncd = NetcdfDataset.openFile(location, null);
     if (useRecords)
       org_ncd.addRecordStructure();

     NcMLWriter writer = new NcMLWriter();

     // create a file and write it out
     int pos = location.lastIndexOf("/");
     String filename = location.substring(pos+1);
     String ncmlOut = TestDataset.xmlDir+ "tmp/"+filename+ ".ncml";
     System.out.println(" output filename= "+ncmlOut);
     try {
       OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlOut, false));
       writer.writeXML( org_ncd, out, null);
       out.close();
     } catch (IOException ioe) {
       ioe.printStackTrace();
       assert false;
     }

     // read it back in
     NetcdfFile new_ncd = NetcdfDataset.openFile(ncmlOut, null);
    if (useRecords)
       new_ncd.addRecordStructure();

     TestCompare.compareFiles( org_ncd, new_ncd);
     //assert cat.equals( catV1);
   }

  private void convertAsNcdataset(String location, boolean useRecords) throws IOException {
    location = StringUtil.replace(location, '\\', "/");

    NetcdfDataset org_ncd = NetcdfDataset.openDataset(location);
    if (useRecords)
       org_ncd.addRecordStructure();

    NcMLWriter writer = new NcMLWriter();
    System.out.println("-----------");
    System.out.println("DS input filename= "+location);

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filename = location.substring(pos+1);
    String ncmlOut = TestDataset.xmlDir+ "tmp/"+filename+ ".ncml";
    System.out.println(" output filename= "+ncmlOut);
    try {
      OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlOut, false));
      writer.writeXML( org_ncd, out, null);
      out.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert false;
    }

    // read it back in
    NetcdfDataset new_ncd = NetcdfDataset.openDataset(ncmlOut);
    if (useRecords)
       new_ncd.addRecordStructure();
    TestCompare.compareFiles( org_ncd, new_ncd);
    //assert cat.equals( catV1);
  }

  /* cant do a hashCode compare

  private void compareDatasets(NetcdfFile org, NetcdfFile ncml) {
    if ((org.getId() != null) || (ncml.getId() != null))
      assert org.getId().equals( ncml.getId());
    if ((org.getTitle() != null) || (ncml.getTitle() != null))
      assert org.getTitle().equals( ncml.getTitle());

    compareGroups( org.getRootGroup(), ncml.getRootGroup());
  }

  private void compareGroups(Group org, Group ncml) {
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

  private void compareVariables(Variable org, Variable ncml) {
    if (showCompare) System.out.println("compareVariables  "+org.getName()+" "+ncml.getName());
    assert org.getName().equals( ncml.getName());

    // dimensions
    checkAll( org.getDimensions(), ncml.getDimensions());

    // attributes
    checkAll( org.getAttributes(), ncml.getAttributes());

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
  public ArrayList checkAll(List list1, List list2) {
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

  public void checkEach(Object want1, List list1, List list2, List result) {
    int index2 = list2.indexOf( want1);
    assert (index2 >= 0) : want1 + " not in list 2";
    Object want2 = list2.get( index2);

    int index1 = list1.indexOf( want2);
    assert (index1 >= 0) : want2 + " not in list 1";
    Object want = list1.get( index1);
    assert want == want1: want1 + " not == "+ want;

    if (showEach) System.out.println("  OK <"+want1+". equals <"+want2+">");
    if (result != null) {
      result.add(want1);
      result.add(want2);
    }
  }

  public static void main(String[] args) throws IOException {
    String urls = "C:/data/conventions/coards/cldc.mean.nc";
    TestCompareNcmlWriter t = new TestCompareNcmlWriter("dummy");
  } */
}
