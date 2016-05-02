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

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Date;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcGrib extends TestCase {
  private boolean showValues = false;


  public void testSimple() throws Exception {
    // no fmrcDefinition
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunCollection' timeUnitsChange='true'>\n" +
      "    <scan location='" + TestDir.cdmUnitTestDir + "ncml/nc/nam_c20s/' suffix='.grib1' " +
            "dateFormatMark='NAM_CONUS_20km_surface_#yyyyMMdd_HHmm'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String location = TestDir.cdmUnitTestDir + "ncml/nc/nam_c20s/fmrcAgg.ncml";
    System.out.printf("%s%n%s%n", location, xml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), location, null);
    TestDir.showMem("TestAggFmrcGrib start ");

    int naggs = 8;

    testDimensions(ncfile, naggs, "time");
    testCoordVar(ncfile, 257);
    int[] runhours = new int[] {0,12,18,24,30, 4194, 4200, 4206};
    double[][] timevals = new double[][]  {
    {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0},
    {12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0},
    {18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0},
    {24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, Double.NaN},
    {30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0},
    {4194.0, 4197.0, 4200.0, 4203.0, 4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0, 4230.0, 4233.0, 4236.0, 4239.0, 4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0, 4266.0, 4269.0, 4272.0, 4275.0, 4278.0},
    {4200.0, 4203.0, 4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0, 4230.0, 4233.0, 4236.0, 4239.0, 4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0, 4266.0, 4269.0, 4272.0, 4275.0, 4278.0, 4281.0, 4284.0},
    {4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0, 4230.0, 4233.0, 4236.0, 4239.0, 4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0, 4266.0, 4269.0, 4272.0, 4275.0, 4278.0, 4281.0, 4284.0, 4287.0, 4290.0}
  };

    testAggCoordVar(ncfile, naggs, new DateUnit("hours since 2006-03-15T18:00:00Z"), runhours);
    testTimeCoordVar(ncfile, naggs, 29, "Pressure_surface", timevals);

    System.out.println("TestAggForecastModel.testReadData ");    
    testReadData(ncfile, naggs);
 //   testReadSlice(ncfile);

    TestDir.showMem("TestAggFmrcGrib end ");
    ncfile.close();    
  }

  // this has fmrcDefinition, and ragged time coords - some are set to NaNs
  public void testRagged() throws Exception {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunCollection' timeUnitsChange='true' " +
            "fmrcDefinition='" + TestDir.cdmUnitTestDir + "ncml/nc/c20ss/fmrcDefinition.xml'>\n" +
      "    <scan location='" + TestDir.cdmUnitTestDir + "ncml/nc/c20ss/' suffix='.grib1' enhance='true' " +
            "dateFormatMark='NAM_CONUS_20km_selectsurface_#yyyyMMdd_HHmm'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";

        double[][] evals = new double[][]   {
    {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
    {6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0},
    {12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
    {18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0}
  };

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), "AggFmrcGribRunseq.ncml", null);
    int naggs = 4;
    String timeVarName = "time";
    String timeDimName = "time";
    testDimensions(ncfile, naggs, timeDimName);
    testCoordVar(ncfile, 257);
    int[] runtimes = new int[] {0,6,12,18};
    testAggCoordVar(ncfile, naggs, new DateUnit("hours since 2006-07-29T18:00:00Z"), runtimes);
    testTimeCoordVar(ncfile, naggs, 29, timeVarName, evals);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg, String timeDimName) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getShortName().equals("x");
    assert latDim.getLength() == 369;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getShortName().equals("y");
    assert lonDim.getLength() == 257;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension(timeDimName);
    assert null != timeDim;
    assert timeDim.getShortName().equals(timeDimName);
    assert timeDim.getLength() == 29;

    Dimension runDim = ncfile.findDimension("run");
    assert null != runDim;
    assert runDim.getShortName().equals("run");
    assert runDim.getLength() == nagg : nagg +" != "+ runDim.getLength();
  }

 private void testCoordVar(NetcdfFile ncfile, int n) throws IOException {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getShortName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == n;
    assert lat.getShape()[0] == n;
    assert lat.getDataType().isFloatingPoint();

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("y"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("km");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

   Array data = lat.read();
   assert data.getRank() == 1;
   assert data.getSize() == n;
   assert data.getShape()[0] == n;
   assert data.getElementType() == double.class || data.getElementType() == float.class;

   int last =(int)  data.getSize() - 1;
   assert Misc.closeEnough(data.getDouble(0), -832.2073364257812) : data.getDouble(0);
   assert Misc.closeEnough(data.getDouble(last), 4369.20068359375) : data.getDouble(last);
  }

  private void testAggCoordVar(NetcdfFile ncfile, int nagg, DateUnit du, int[] runhours) {
    Variable time = ncfile.findVariable("run");
    assert null != time;
    assert time.getShortName().equals("run");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.DOUBLE;

    DateFormatter formatter = new DateFormatter();
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == double.class;

      NCdumpW.printArray(data);

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        assert val == runhours[count];
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int ntimes, String varName, double[][] timevals) throws Exception {
    Variable v = ncfile.findVariable(varName);
    assert v != null : ncfile.getLocation();
    Dimension d = v.getDimension(1); // time dim
    Variable time = ncfile.findVariable(d.getShortName());
    assert null != time;
    System.out.printf("%ntime dimension for %s = %s%n", varName, time.getFullName());

    assert time.getRank() == 2;
    assert time.getSize() == nagg * ntimes;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == ntimes;
    assert time.getDataType() == DataType.DOUBLE || time.getDataType() == DataType.INT;

    String units = time.getUnitsString();
    DateUnit du = new DateUnit( units);

    DateFormatter formatter = new DateFormatter();
    Array data = time.read();
    if (true) {
      PrintWriter pw = new PrintWriter(System.out);
      NCdumpW.printArray(data, "timeCoords", pw, null);
      pw.flush();
    }

    assert data.getSize() == nagg * ntimes;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == ntimes;
    assert data.getElementType() == double.class || data.getElementType() == int.class;

    while (data.hasNext()) {
      double val = data.nextDouble();
      Date date = du.makeDate(val);
      // if (showValues) System.out.println(" date= "+ formatter.toDateTimeStringISO(date));
    }

    Index ima = data.getIndex();
    for (int run=0; run<nagg; run++)
      for (int tidx=0; tidx<ntimes; tidx++) {
        double val = data.getDouble(ima.set(run, tidx));
        if (showValues) System.out.println(" run= "+ run + " tidx= "+ tidx +  " val= "+ val );
        if (!Double.isNaN(val))
          assert Misc.closeEnough(val, timevals[run][tidx]) : "run,time=("+run+","+tidx+"): "+val+" != "+ timevals[run][tidx];
      }

  }

  private void testReadData(NetcdfFile ncfile, int nagg) throws IOException {
    Variable v = ncfile.findVariable("Pressure_surface");
    assert null != v;
    assert v.getShortName().equals("Pressure_surface");  // float Pressure_surface(run=8, time=29, y=257, x=369);
    assert v.getRank() == 4;
    int[] shape = v.getShape();
    assert shape[0] == nagg;
    assert shape[1] == 29 : new Section(shape).toString();
    assert shape[2] == 257 : new Section(shape).toString();
    assert shape[3] == 369 : new Section(shape).toString();
    assert v.getDataType() == DataType.FLOAT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("run");
    assert v.getDimension(1) == ncfile.findDimension("time");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

    Array data = v.read();
    assert data.getRank() == 4;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == 29;
    assert data.getShape()[2] == 257;
    assert data.getShape()[3] == 369;

    double sum = MAMath.sumDoubleSkipMissingData(data, 0.0);

    /* float sum = 0.0f;
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      sum += ii.getFloatNext();
    } */
    System.out.println(" sum= "+sum);

  }

  private void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 4;
      assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getShape()[3] == shape[3];
      assert data.getElementType() == float.class;

      /* Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100*(i+origin[0]) + 10*j + k) : val;
        } */

  }

  private void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {1, 11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {3, 2, 3, 2} );
    testReadSlice( ncfile, new int[] {3, 5, 0, 0}, new int[] {1, 5, 3, 4} );
    testReadSlice( ncfile, new int[] {3, 9, 0, 0}, new int[] {5, 2, 2, 3} );
   }
}

