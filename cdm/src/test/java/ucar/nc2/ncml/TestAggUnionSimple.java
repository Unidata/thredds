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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Misc;

import java.io.IOException;

/**
 * Test agg union
 */

/*
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
	<attribute name="title" type="string" value="Union cldc and lflx"/>
  <aggregation type="union">
    <netcdf location="file:src/test/data/ncml/nc/cldc.mean.nc"/>
    <netcdf location="file:src/test/data/ncml/nc/lflx.mean.nc"/>
  </aggregation>
</netcdf>

netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/cldc.mean.nc {
 dimensions:
   time = UNLIMITED;   // (456 currently)
   lat = 21;
   lon = 360;
 variables:
   float lat(lat=21);
     :long_name = "Latitude";
     :units = "degrees_north";
     :actual_range = 10.0f, -10.0f; // float
   float lon(lon=360);
     :long_name = "Longitude";
     :units = "degrees_east";
     :actual_range = 0.5f, 359.5f; // float
   double time(time=456);
     :units = "days since 1-1-1 00:00:0.0";
     :long_name = "Time";
     :delta_t = "0000-01-00 00:00:00";
     :avg_period = "0000-01-00 00:00:00";
     :actual_range = 715511.0, 729360.0; // double
   short cldc(time=456, lat=21, lon=360);
     :valid_range = 0.0f, 8.0f; // float
     :actual_range = 0.0f, 8.0f; // float
     :units = "okta";
     :precision = 1s; // short
     :missing_value = 32766s; // short
     :_FillValue = 32766s; // short
     :long_name = "Cloudiness Monthly Mean at Surface";
     :dataset = "COADS 1-degree Equatorial Enhanced\nAI";
     :var_desc = "Cloudiness\nC";
     :level_desc = "Surface\n0";
     :statistic = "Mean\nM";
     :parent_stat = "Individual Obs\nI";
     :add_offset = 3276.5f; // float
     :scale_factor = 0.1f; // float

 :title = "COADS 1-degree Equatorial Enhanced";
 :history = "";
 :Conventions = "COARDS";
}

netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/lflx.mean.nc {
 dimensions:
   time = UNLIMITED;   // (456 currently)
   lat = 21;
   lon = 360;
 variables:
   float lat(lat=21);
     :long_name = "Latitude";
     :units = "degrees_north";
     :actual_range = 10.0f, -10.0f; // float
   float lon(lon=360);
     :long_name = "Longitude";
     :units = "degrees_east";
     :actual_range = 0.5f, 359.5f; // float
   double time(time=456);
     :units = "days since 1-1-1 00:00:0.0";
     :long_name = "Time";
     :delta_t = "0000-01-00 00:00:00";
     :avg_period = "0000-01-00 00:00:00";
     :actual_range = 715511.0, 729360.0; // double
   short lflx(time=456, lat=21, lon=360);
     :valid_range = -1000.0f, 1000.0f; // float
     :actual_range = -88.700005f, 236.1f; // float
     :units = "grams/kg m/s";
     :precision = 1s; // short
     :missing_value = 32766s; // short
     :_FillValue = 32766s; // short
     :long_name = "Latent Heat Parameter Monthly Mean at Surface";
     :dataset = "COADS 1-degree Equatorial Enhanced\nAI";
     :var_desc = "Latent Heat Parameter\nG";
     :level_desc = "Surface\n0";
     :statistic = "Mean\nM";
     :parent_stat = "Individual Obs\nI";
     :add_offset = 2276.5f; // float
     :scale_factor = 0.1f; // float

 :title = "COADS 1-degree Equatorial Enhanced";
 :history = "";
 :Conventions = "COARDS";
}
*/

public class TestAggUnionSimple extends TestCase {

  public TestAggUnionSimple(String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;

  public void setUp() {
    if (ncfile != null) return;
    String filename = "file:./" + TestNcML.topDir + "aggUnionSimple.xml";

    try {
      ncfile = NetcdfDataset.openDataset(filename, false, null);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = " + e);
    } catch (IOException e) {
      System.out.println("IO error = " + e);
      e.printStackTrace();
      assert false;
    }
  }

  public void tearDown() throws IOException {
    if (ncfile != null) ncfile.close();
    ncfile = null;
  }

  public void testDataset() {
    Variable v = ncfile.findVariable("lflx");
    assert v instanceof VariableDS;
    VariableDS vds = (VariableDS) v;
    assert vds.getOriginalDataType() == v.getDataType();

    Variable org = vds.getOriginalVariable();
    assert vds.getOriginalDataType() == org.getDataType();

    assert !(org instanceof VariableDS);

    assert v.getParentGroup().equals(org.getParentGroup());
    assert v.getParentGroup() != org.getParentGroup();
  }

  public void testRead() {
    System.out.println("ncfile = " + ncfile);
    ucar.nc2.TestUtils.testReadData(ncfile, true);
  }

  public void testStructure() {
    System.out.println("TestNested = \n" + ncfile);

    Attribute att = ncfile.findGlobalAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Union cldc and lflx");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 21;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 456;
    assert timeDim.isUnlimited();
  }

  public void testReadCoordvar() {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 21;
    assert lat.getShape()[0] == 21;
    assert lat.getDataType() == DataType.FLOAT;

    assert lat.isCoordinateVariable();
    assert !lat.isUnlimited();

    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 21;
      assert data.getShape()[0] == 21;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert Misc.closeEnough(dataI.getDoubleNext(), 10.0);
      assert Misc.closeEnough(dataI.getDoubleNext(), 9.0);
      assert Misc.closeEnough(dataI.getDoubleNext(), 8.0);
    } catch (IOException io) {
    }

  }

  public void testReadData() {

    Variable v = ncfile.findVariable("lflx");
    assert null != v;
    assert v.getShortName().equals("lflx");
    assert v.getRank() == 3;
    assert v.getSize() == 360 * 21 * 456;
    assert v.getShape()[0] == 456;
    assert v.getShape()[1] == 21;
    assert v.getShape()[2] == 360;
    assert v.getDataType() == DataType.SHORT : v.getDataType();

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("grams/kg m/s");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 360 * 21 * 456;
      assert data.getShape()[0] == 456;
      assert data.getShape()[1] == 21;
      assert data.getShape()[2] == 360;
      assert data.getElementType() == short.class;

      IndexIterator dataI = data.getIndexIterator();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
    } catch (IOException io) {
    }
  }

  public void testReadSlice() {

    Variable v = ncfile.findVariable("lflx");
    int[] origin = {0, 6, 5};
    int[] shape = {1, 2, 3};
    try {
      Array data = v.read(origin, shape).reduce();
      assert data.getRank() == 2;
      assert data.getSize() == 6;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getElementType() == short.class;

      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getShortNext() == -22711;
      assert dataI.getShortNext() == -22239;
      assert dataI.getShortNext() == -22585;
      assert dataI.getShortNext() == -22670;
      assert dataI.getShortNext() == 32766;
      assert dataI.getShortNext() == 32766;
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  /* test that scanning gives the exact same result
  <aggregation type="union">
    <scan location="file:src/test/data/ncml/nc/" suffix="mean.nc"/>
  </aggregation>
  */
  public void testScan() throws IOException {
    String filename = "file:./" + TestNcML.topDir + "aggUnionScan.xml";
    NetcdfDataset scanFile = NetcdfDataset.openDataset(filename, false, null);
    ucar.unidata.util.test.CompareNetcdf.compareFiles(ncfile, scanFile, true, true, false);
    scanFile.close();
  }

  public void testRename() throws IOException {
    String filename = "file:./" + TestNcML.topDir + "aggUnionRename.xml";
    NetcdfDataset scanFile = NetcdfDataset.openDataset(filename, false, null);
    Variable v = scanFile.findVariable("LavaFlow");
    assert v != null;
    scanFile.close();
  }

}
