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

import ucar.nc2.dataset.NetcdfDataset;

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

  private void testEquals(String ncmlLocation) throws  IOException {
    System.out.println("testEquals");
    NetcdfDataset ncd = NcMLReader.readNcML(ncmlLocation, null);

    String locref  = ncd.getReferencedFile().getLocation();
    NetcdfDataset ncdref = NetcdfDataset.openDataset(locref, false, null);

    ucar.unidata.util.test.CompareNetcdf.compareFiles(ncd, ncdref, false, true, false);

    ncd.close();
    ncdref.close();
  }

  private void testEnhanceEquals(String ncmlLocation) throws  IOException {
    System.out.println("testEnhanceEquals");
    NetcdfDataset ncml = NcMLReader.readNcML(ncmlLocation, null);
    NetcdfDataset ncmlEnhanced = new NetcdfDataset(ncml, true);

    String locref  = ncml.getReferencedFile().getLocation();
    NetcdfDataset ncdrefEnhanced = NetcdfDataset.openDataset(locref, true, null);

    ucar.unidata.util.test.CompareNetcdf.compareFiles(ncmlEnhanced, ncdrefEnhanced, false, true, false);

    ncml.close();
    ncdrefEnhanced.close();
  }



}
