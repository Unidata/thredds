/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test agg union.
 * the 2 files are copies, use explicit to mask one and rename the other.
 */

/*
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">

  <attribute name="title" type="string" value="Example Data"/>

  <aggregation type="union">
    <netcdf location="file:src/test/data/ncml/nc/example2.nc">
      <explicit/>

      <dimension name="time" length="2" isUnlimited="true"/>
      <dimension name="lat" length="3"/>
      <dimension name="lon" length="4"/>

      <variable name="lat" type="float" shape="lat">
        <attribute name="units" type="string" value="degrees_north"/>
      </variable>
      <variable name="lon" type="float" shape="lon">
        <attribute name="units" type="string" value="degrees_east"/>
      </variable>
      <variable name="time" type="int" shape="time">
        <attribute name="units" type="string" value="hours"/>
      </variable>

      <variable name="ReletiveHumidity" type="int" shape="time lat lon" orgName="rh">
        <attribute name=CDM.LONG_NAME type="string" value="relative humidity"/>
        <attribute name="units" type="string" value="percent"/>
      </variable>
    </netcdf>

    <netcdf location="file:src/test/data/ncml/nc/example1.nc">
      <explicit/>

      <dimension name="time" length="2" isUnlimited="true"/>
      <dimension name="lat" length="3"/>
      <dimension name="lon" length="4"/>

      <variable name="Temperature" type="double" shape="time lat lon" orgName="T">
        <attribute name=CDM.LONG_NAME type="string" value="surface temperature"/>
        <attribute name="units" type="string" value="degC"/>
      </variable>
    </netcdf>

  </aggregation>
</netcdf>

netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/example1.nc {
 dimensions:
   time = UNLIMITED;   // (2 currently)
   lat = 3;
   lon = 4;
 variables:
   int rh(time=2, lat=3, lon=4);
     :long_name = "relative humidity";
     :units = "percent";
   double T(time=2, lat=3, lon=4);
     :long_name = "surface temperature";
     :units = "degC";
   float lat(lat=3);
     :units = "degrees_north";
   float lon(lon=4);
     :units = "degrees_east";
   int time(time=2);
     :units = "hours";

 :title = "Example Data";
}

netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/example2.nc {
 dimensions:
   time = UNLIMITED;   // (2 currently)
   lat = 3;
   lon = 4;
 variables:
   int rh(time=2, lat=3, lon=4);
     :long_name = "relative humidity";
     :units = "percent";
   double T(time=2, lat=3, lon=4);
     :long_name = "surface temperature";
     :units = "degC";
   float lat(lat=3);
     :units = "degrees_north";
   float lon(lon=4);
     :units = "degrees_east";
   int time(time=2);
     :units = "hours";

 :title = "Example Data";
}
 */

public class TestAggUnion extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestAggUnion(String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;

  public void setUp() throws IOException {
    if (ncfile != null) return;
    String filename = "file:./" + TestNcML.topDir + "aggUnion.xml";
    ncfile = NcMLReader.readNcML(filename, null);
  }

  public void tearDown() throws IOException {
    if (ncfile != null) ncfile.close();
    ncfile = null;
  }

  public void testDataset() {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert v instanceof VariableDS;
    VariableDS vds = (VariableDS) v;
    assert vds.getOriginalDataType() == v.getDataType();

    Variable org = vds.getOriginalVariable();
    assert vds.getOriginalDataType() == org.getDataType();

    assert v.getParentGroup().equals(org.getParentGroup());
    assert v.getParentGroup() != org.getParentGroup();

    // its a VariableDS because the renaming causes a VariableDS wrapper.
    assert (org instanceof VariableDS);

    vds = (VariableDS) org;
    org = vds.getOriginalVariable();
    assert vds.getOriginalDataType() == org.getDataType();
    assert !(org instanceof VariableDS);

    assert v.getParentGroup().equals(org.getParentGroup());
    assert v.getParentGroup() != org.getParentGroup();
  }

  public void testMetadata() {
    logger.debug("TestNested = \n{}", ncfile);

    Attribute att = ncfile.findGlobalAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Example Data");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 2;
    assert timeDim.isUnlimited();
  }

  public void testCoordvar() throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

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
  
    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == float.class;
  
    IndexIterator dataI = data.getIndexIterator();
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 41.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 40.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 39.0);
  }

  public void testReadData() throws IOException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert null != v;
    assert v.getShortName().equals("ReletiveHumidity");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.INT;

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
    assert att.getStringValue().equals("percent");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;
  
    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24 : data.getSize();
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == int.class;
  
    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 2;
    assert dataI.getIntNext() == 3;
    assert dataI.getIntNext() == 4;
    assert dataI.getIntNext() == 5;
  }

  public void testReadSlice() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};
  
    Array data = v.read(origin, shape);
    assert data.getRank() == 3;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 1;
    assert data.getElementType() == int.class;
  
    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 5;
    assert dataI.getIntNext() == 9;
    assert dataI.getIntNext() == 21;
    assert dataI.getIntNext() == 25;
    assert dataI.getIntNext() == 29;
  }

  public void testReadSlice2() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};
  
    Array data = v.read(origin, shape).reduce();
    assert data.getRank() == 2;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getElementType() == int.class;
  
    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 2;
    assert dataI.getIntNext() == 3;
    assert dataI.getIntNext() == 21;
    assert dataI.getIntNext() == 22;
    assert dataI.getIntNext() == 23;
  }

  public void testReadDataAlias() throws IOException {

    Variable v = ncfile.findVariable("T");
    assert null == v;

    v = ncfile.findVariable("Temperature");
    assert null != v;
    assert v.getShortName().equals("Temperature");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

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
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;
  
    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == double.class;
  
    IndexIterator dataI = data.getIndexIterator();
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 1.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 2.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 3.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 4.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 2.0);
  }
}
