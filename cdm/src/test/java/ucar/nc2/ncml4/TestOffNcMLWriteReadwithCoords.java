package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.*;
import ucar.nc2.ncml.NcMLWriter;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.*;

/** test wrinting to NcML, then reading back and comparing to original.
 * use addCoords.
 * */

public class TestOffNcMLWriteReadwithCoords extends TestCase {
  static boolean showFiles = true, showCompare = false, showEach = false;

  public TestOffNcMLWriteReadwithCoords( String name) {
    super(name);
  }

  private ArrayList files;
  private String griddir = TestAll.upcShareTestDataDir + "grid/netcdf/";

  public void setUp() {
    files = new ArrayList();

    files.add( griddir+"atd-radar/rgg.20020411.000000.lel.ll.nc");
    files.add( griddir+"cf/ccsm2.nc"); //
    files.add( griddir+"coards/cldc.mean.nc");
    files.add( griddir+"csm/o3monthly.nc");
    files.add( griddir+"gdv/OceanDJF.nc");
    files.add( griddir+"gief/coamps.wind_uv.nc");
    files.add( griddir+"mars/temp_air_01082000.nc");
    files.add( griddir+"nuwg/eta.nc");
    files.add( griddir+"nuwg/ocean.nc");
    files.add( griddir+"wrf/wrfout_v2_Lambert.nc");
    files.add( griddir+"atd-radar/SPOL_3Volumes.nc"); // 

    files.add( TestAll.upcShareTestDataDir + "grid/grib/grib2/data/eta2.wmo"); //
    files.add( TestAll.upcShareTestDataDir + "grid/grib/grib2/data/ndfd.wmo"); //

    files.add( TestAll.upcShareTestDataDir + "satellite/gini/n0r_20041013_1852-compress"); //
    files.add( TestAll.upcShareTestDataDir + "satellite/gini/ntp_20041206_2154"); //
    files.add( TestAll.upcShareTestDataDir + "satellite/dmsp/F14200307192230.n.OIS"); // 

    files.add( TestAll.upcShareTestDataDir + "radar/nexrad/level2/6500KHGX20000610_000110.Z"); // */
    files.add( TestAll.upcShareTestDataDir + "radar/nexrad/level2/Level2_KYUX_20060527_2335.ar2v");
  }


  public void testReadAsNcdataset() throws Exception {
    for (int i = 0; i < files.size(); i++) {
      String s = (String) files.get(i);
      convertAsNcdataset(s);
    }
  }

  public void utestOne() throws Exception  {
    convertAsNcdataset( "//zero/share/testdata/grid/grib/grib2/data/ndfd.wmo");
  }

  private void convertAsNcdataset(String location) throws IOException {
    location = StringUtil.replace(location, '\\', "/");

    NetcdfDataset org_ncd = NetcdfDatasetCache.acquire(location, null);
    NcMLWriter writer = new NcMLWriter();
    if (showFiles) {
      System.out.println("-----------");
      System.out.println("DS input filename= "+location);
    }

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String filename = location.substring(pos+1);
    String ncmlOut = TestAll.temporaryDataDir + filename + ".ncml";
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
    NetcdfDataset new_ncd = NetcdfDataset.openDataset(ncmlOut, true, null);
    TestCompare.compareFiles( org_ncd, new_ncd);
    //assert cat.equals( catV1);

    org_ncd.close();
    new_ncd.close();
  }

  /* cant do a hashCode compare

  static public void compareDatasets(NetcdfDataset org, NetcdfDataset ncml) {
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
      VariableEnhanced orgV =  (VariableEnhanced) vars.get(i);
      VariableEnhanced ncmlV =  (VariableEnhanced) vars.get(i+1);
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

  static void compareVariables(VariableEnhanced org, VariableEnhanced ncml) {
    if (showCompare) System.out.println("compareVariables  "+org.getName()+" "+ncml.getName());
    assert org.getName().equals( ncml.getName());

    // dimensions
    checkAll( org.getDimensions(), ncml.getDimensions());

    // attributes
    checkAll( org.getAttributes(), ncml.getAttributes());

    // coord sys
    checkAll( org.getCoordinateSystems(), ncml.getCoordinateSystems());


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
        VariableEnhanced orgV =  (VariableEnhanced) vars.get(i);
        VariableEnhanced ncmlV =  (VariableEnhanced) vars.get(i+1);
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
    if (index2 < 0)
      System.out.println(); // grab in debugger

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
    TestNcMLWriteReadwithCoords t = new TestNcMLWriteReadwithCoords("dummy");
  } */
}
