package ucar.nc2.ncml;

import junit.framework.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLequals extends TestCase {

  public TestNcMLequals( String name) {
    super(name);
  }

  public void testEquals() throws IOException {
    testEquals("file:"+TestNcML.topDir + "testEquals.xml");
    testEnhanceEquals("file:"+TestNcML.topDir + "testEquals.xml");
  }

  public void problem() throws IOException {
    //testEnhanceEquals("file:G:/zg500_MM5I_1979010103.ncml");
    testEquals("file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
    testEnhanceEquals("file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
  }

  private void testEquals(String ncmlLocation) throws IOException {
    System.out.println("testEquals");
    NetcdfDataset ncd = NcMLReader.readNcML(ncmlLocation, null);

    String locref  = ncd.getReferencedFile().getLocation();
    NetcdfDataset ncdref = NetcdfDataset.openDataset(locref, false, null);

    TestCompare.compareFiles(ncd, ncdref, false, true, false);

    ncd.close();
    ncdref.close();
  }

  private void testEnhanceEquals(String ncmlLocation) throws IOException {
    System.out.println("testEnhanceEquals");
    NetcdfDataset ncml = NcMLReader.readNcML(ncmlLocation, null);
    NetcdfDataset ncmlEnhanced = new NetcdfDataset(ncml, true);

    String locref  = ncml.getReferencedFile().getLocation();
    NetcdfDataset ncdrefEnhanced = NetcdfDataset.openDataset(locref, true, null);

    TestCompare.compareFiles(ncmlEnhanced, ncdrefEnhanced, false, true, false);

    ncml.close();
    ncdrefEnhanced.close();
  }



}
