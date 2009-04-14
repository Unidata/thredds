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
package ucar.nc2.dt.point;

import junit.framework.*;

import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Iterator;

/** Test PointObsDataset adapters in the JUnit framework. */

public class TestPointDataset extends TestCase {
  String topDir = ucar.nc2.TestAll.testdataDir + "point/netcdf/";
  public TestPointDataset( String name) {
    super(name);
  }

  public void testNetcdfDataset() throws IOException {
    testPointMethods( topDir+"Compilation_eq.nc");
    testPointMethods( topDir+"Earthquake_Mag4_Up_eq.nc");
    testPointMethods( topDir+"mags_compilation_eq.nc");
  }

  public void utestDapperDataset() throws IOException {
    testPointMethods( "http://dapper.pmel.noaa.gov/dapper/epic/puget_prof_ctd.cdp");
  }

  private void testPointMethods(String location) throws IOException {
    StringBuilder sbuff = new StringBuilder();
    PointObsDataset pod = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, location, null, sbuff);
    assert pod != null : sbuff.toString();

    System.out.println("-----------");
    System.out.println(pod.getDetailInfo());

    Date d1 = pod.getStartDate();
    Date d2 = pod.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals( d2);

    DateUnit du = pod.getTimeUnits();
    assert null != du;

    double startVal = du.makeValue( d1);
    double endVal = du.makeValue( d2);
    assert startVal <= endVal;

    Class dataClass = pod.getDataClass();
    assert dataClass == PointObsDatatype.class;

    List dataVars =  pod.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != pod.getDataVariable( v.getShortName());
    }

    // make a new bb
    LatLonRect bb = pod.getBoundingBox();
    assert null != bb;
    double h = bb.getUpperRightPoint().getLatitude() - bb.getLowerLeftPoint().getLatitude();
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getWidth()/2, h/2);

    List data = pod.getData( bb2);
    testData( pod.getTimeUnits(), data.iterator());

    // make a new data range
    double diff = endVal - startVal;
    Date startRange = du.makeDate( startVal + .25 * diff);
    Date endRange = du.makeDate( startVal + .75 * diff);
    data = pod.getData( bb2, startRange, endRange);
    testData( pod.getTimeUnits(), data.iterator());

    data = pod.getData( bb2, startRange, endRange);
    testData( pod.getTimeUnits(), data.iterator());

    DataIterator dataIter = pod.getDataIterator(0);
    int count = testData( pod.getTimeUnits(), dataIter);
    System.out.println(" getData size= "+count+" getDataCount= "+pod.getDataCount());

    pod.close();
  }

  private int testData( DateUnit timeUnit, Iterator dataIter) throws java.io.IOException {
    int count = 0;
    while(dataIter.hasNext()) {
      Object data = dataIter.next();
      assert data instanceof PointObsDatatype : data.getClass().getName();
      PointObsDatatype pobs = (PointObsDatatype) data;

      ucar.unidata.geoloc.EarthLocation loc = pobs.getLocation();
      assert loc != null;

      assert null != pobs.getNominalTimeAsDate();
      assert null != pobs.getObservationTimeAsDate();

      assert timeUnit.makeDate( pobs.getNominalTime()).equals( pobs.getNominalTimeAsDate());
      assert timeUnit.makeDate( pobs.getObservationTime()).equals( pobs.getObservationTimeAsDate());

      StructureData sdata = pobs.getData();
      assert null != sdata;
      testData( sdata);
      count++;
    }
    return count;
  }

  private void testData( StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if ((dt != DataType.STRING) && (dt != DataType.STRUCTURE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }


}
