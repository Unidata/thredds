/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLequals extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  private void testEquals(String ncmlLocation) throws  IOException {
    System.out.println("testEquals");
    NetcdfDataset ncd = NcMLReader.readNcML(ncmlLocation, null);

    String locref  = ncd.getReferencedFile().getLocation();
    NetcdfDataset ncdref = NetcdfDataset.openDataset(locref, false, null);

    ucar.unidata.util.test.CompareNetcdf.compareFiles(ncd, ncdref, false, false, false);

    ncd.close();
    ncdref.close();
  }

  private void testEnhanceEquals(String ncmlLocation) throws  IOException {
    System.out.println("testEnhanceEquals");
    NetcdfDataset ncml = NcMLReader.readNcML(ncmlLocation, null);
    NetcdfDataset ncmlEnhanced = new NetcdfDataset(ncml, true);

    String locref  = ncml.getReferencedFile().getLocation();
    NetcdfDataset ncdrefEnhanced = NetcdfDataset.openDataset(locref, true, null);

    ucar.unidata.util.test.CompareNetcdf.compareFiles(ncmlEnhanced, ncdrefEnhanced, false, false, false);

    ncml.close();
    ncdrefEnhanced.close();
  }



}
