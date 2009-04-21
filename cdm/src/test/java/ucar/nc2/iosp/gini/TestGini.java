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
package ucar.nc2.iosp.gini;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;


public class TestGini extends TestCase {

  public static String compressGini = TestAll.cdmLocalTestDataDir +"gini/n0r_20041013_1852-compress";
  public static String uncompressGini = TestAll.cdmLocalTestDataDir +"gini/n0r_20041013_1852-uncompress";
  public static String compress_n1p = TestAll.cdmLocalTestDataDir +"gini/n1p_20041206_2140";
  public static String compress_ntp = TestAll.cdmLocalTestDataDir +"gini/ntp_20041206_2154";
  public static String satelliteAK_IR = TestAll.cdmLocalTestDataDir +"gini/AK-NATIONAL_8km_IR_20050912_2345.gini";
  public static String satellitePR_IR = TestAll.cdmLocalTestDataDir +"gini/PR-REGIONAL_4km_12.0_20050922_0600.gini";
  public static String satelliteHI10km_Sound = TestAll.cdmLocalTestDataDir +"gini/HI-NATIONAL_10km_SOUND-6.51_20050918_1824.gini";
  public static String satelliteHI14km_IR = TestAll.cdmLocalTestDataDir +"gini/HI-NATIONAL_14km_IR_20050918_2000.gini";
  public static String satelliteHI4km_IR = TestAll.cdmLocalTestDataDir +"gini/HI-REGIONAL_4km_IR_20050919_1315.gini";
  public static String satelliteSuper1km_PW = TestAll.cdmLocalTestDataDir +"gini/SUPER-NATIONAL_1km_PW_20050923_1400.gini";
  public static String satelliteSuper1km_SFT = TestAll.cdmLocalTestDataDir +"gini/SUPER-NATIONAL_1km_SFC-T_20050912_1900.gini";
  public static String satelliteSuper8km_IR = TestAll.cdmLocalTestDataDir +"gini/SUPER-NATIONAL_8km_IR_20050911_2345.gini";
  public static String satelliteEast_4km_12 = TestAll.cdmLocalTestDataDir +"gini/EAST-CONUS_4km_12.0_20050912_0600.gini";
  public static String satelliteEast_8km_13 = TestAll.cdmLocalTestDataDir +"gini/EAST-CONUS_8km_13.3_20050912_2240.gini";
  public static String satelliteWest_4km_39 = TestAll.cdmLocalTestDataDir +"gini/WEST-CONUS_4km_3.9_20050912_2130.gini";


  public static boolean dumpFile = false;

  public void testGiniReadCompressed() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;

      System.out.println("**** Open " + compressGini);
      ncfile = NetcdfFile.open(compressGini);
      v = ncfile.findVariable("Reflectivity");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();
  }

  public void testGiniReadUnCompressed() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + uncompressGini);
      ncfile = NetcdfFile.open(uncompressGini);
      v = ncfile.findVariable("Reflectivity");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));


      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadCompress_n1p() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + compress_n1p);
      ncfile = NetcdfFile.open(compress_n1p);
      v = ncfile.findVariable("Precipitation");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));


      testReadData(v);
      ncfile.close();

  }

  public void testGiniReadCompress_ntp() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + compress_ntp);
      ncfile = NetcdfFile.open(compress_ntp);
      v = ncfile.findVariable("Precipitation");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));


      testReadData(v);
      ncfile.close();



  }

  public void testGiniReadSatelliteAK_IR() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteAK_IR);
      ncfile = NetcdfFile.open(satelliteAK_IR);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));


      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadSatellitePR_IR() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satellitePR_IR);
      ncfile = NetcdfFile.open(satellitePR_IR);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();

  }

  public void testGiniReadSatelliteHI10km_Sound() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteHI10km_Sound);
      ncfile = NetcdfFile.open(satelliteHI10km_Sound);
      v = ncfile.findVariable("sounder_imagery");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadSatelliteHI14km_IR() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteHI14km_IR);
      ncfile = NetcdfFile.open(satelliteHI14km_IR);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();



  }

  public void testGiniReadSatelliteHI4km_IR()throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteHI4km_IR);
      ncfile = NetcdfFile.open(satelliteHI4km_IR);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadSatelliteSuper1km_PW() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteSuper1km_PW);
      ncfile = NetcdfFile.open(satelliteSuper1km_PW);
      v = ncfile.findVariable("PW");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();



  }

  public void testGiniReadSatelliteSuper1km_SFT() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteSuper1km_SFT);
      ncfile = NetcdfFile.open(satelliteSuper1km_SFT);
      v = ncfile.findVariable("SFC_T");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();

  }

  public void testGiniReadSatelliteSuper8km_IR() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteSuper8km_IR);
      ncfile = NetcdfFile.open(satelliteSuper8km_IR);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();

  }

  public void testGiniReadSatelliteEast_4km_12()throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteEast_4km_12);
      ncfile = NetcdfFile.open(satelliteEast_4km_12);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));

      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadSatelliteEast_8km_13() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteEast_8km_13);
      ncfile = NetcdfFile.open(satelliteEast_8km_13);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));
      testReadData(v);
      ncfile.close();


  }

  public void testGiniReadSatelliteWest_4km_39() throws IOException {
    NetcdfFile ncfile;
    Variable v = null;
      System.out.println("**** Open " + satelliteWest_4km_39);
      ncfile = NetcdfFile.open(satelliteWest_4km_39);
      v = ncfile.findVariable("IR");
      assert(null != v.getDimension(0));
      assert(null != v.getDimension(1));
      testReadData(v);
      ncfile.close();


  }

  private void testReadData(Variable v) throws IOException {
    Array a = null;
    assert(null != v);
    assert(null != v.getDimension(0));
      a = v.read();
      assert(null != a);

    assert(v.getSize() == a.getSize());
  }
}


