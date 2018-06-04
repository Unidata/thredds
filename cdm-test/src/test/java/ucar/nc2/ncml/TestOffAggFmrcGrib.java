/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcGrib {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void testSimple() throws Exception {
        String location = TestDir.cdmUnitTestDir + "ncml/nc/nam_c20s/fmrcAgg.ncml";
        logger.debug("{}", location);

        // no fmrcDefinition
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                "  <aggregation dimName='run' type='forecastModelRunCollection' timeUnitsChange='true'>\n" +
                "    <scan location='" + TestDir.cdmUnitTestDir + "ncml/nc/nam_c20s/' suffix='.grib1' " +
                "dateFormatMark='NAM_CONUS_20km_surface_#yyyyMMdd_HHmm'/>\n" + "  </aggregation>\n" + "</netcdf>";
        logger.debug("{}", xml);

        int naggs = 8;
        int[] runhours = new int[] { 0, 12, 18, 24, 30, 4194, 4200, 4206 };
        double[][] timevals = new double[][] {
                { 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0,
                        51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0 },
                { 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0,
                        63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0 },
                { 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0,
                        69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0 },
                { 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0,
                        78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, Double.NaN },
                { 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0,
                        81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0 },
                { 4194.0, 4197.0, 4200.0, 4203.0, 4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0,
                        4230.0, 4233.0, 4236.0, 4239.0, 4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0,
                        4266.0, 4269.0, 4272.0, 4275.0, 4278.0 },
                { 4200.0, 4203.0, 4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0, 4230.0, 4233.0,
                        4236.0, 4239.0, 4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0, 4266.0, 4269.0,
                        4272.0, 4275.0, 4278.0, 4281.0, 4284.0 },
                { 4206.0, 4209.0, 4212.0, 4215.0, 4218.0, 4221.0, 4224.0, 4227.0, 4230.0, 4233.0, 4236.0, 4239.0,
                        4242.0, 4245.0, 4248.0, 4251.0, 4254.0, 4257.0, 4260.0, 4263.0, 4266.0, 4269.0, 4272.0, 4275.0,
                        4278.0, 4281.0, 4284.0, 4287.0, 4290.0 } };

        try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), location, null)) {
            logger.debug(showMem("Start "));

            testDimensions(ncfile, naggs, "time");
            testCoordVar(ncfile, 257);

            testAggCoordVar(ncfile, naggs, new DateUnit("hours since 2006-03-15T18:00:00Z"), runhours);
            testTimeCoordVar(ncfile, naggs, 29, "Pressure_surface", timevals);

            testReadData(ncfile, naggs);
            //   testReadSlice(ncfile);

            logger.debug(showMem("End "));
        }
    }

    private static String showMem(String where) {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder sb = new StringBuilder();
        sb.append(where).append(" memory free = ").append(runtime.freeMemory() * .001 * .001);
        sb.append(" total= ").append(runtime.totalMemory() * .001 * .001);
        sb.append(" max= ").append(runtime.maxMemory() * .001 * .001).append(" MB");
        return sb.toString();
    }

    // this has fmrcDefinition, and ragged time coords - some are set to NaNs
    @Test
    public void testRagged() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                "  <aggregation dimName='run' type='forecastModelRunCollection' timeUnitsChange='true' " +
                "fmrcDefinition='" + TestDir.cdmUnitTestDir + "ncml/nc/c20ss/fmrcDefinition.xml'>\n" +
                "    <scan location='" + TestDir.cdmUnitTestDir + "ncml/nc/c20ss/' suffix='.grib1' enhance='true' " +
                "dateFormatMark='NAM_CONUS_20km_selectsurface_#yyyyMMdd_HHmm'/>\n" + "  </aggregation>\n" + "</netcdf>";

        double[][] evals = new double[][] {
                { 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0,
                        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, Double.NaN, Double.NaN, Double.NaN },
                { 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0,
                        57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0 },
                { 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0,
                        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, Double.NaN, Double.NaN, Double.NaN },
                { 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0,
                        69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0 } };

        try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), "AggFmrcGribRunseq.ncml", null)) {
            int naggs = 4;
            String timeVarName = "time";
            String timeDimName = "time";
            testDimensions(ncfile, naggs, timeDimName);
            testCoordVar(ncfile, 257);
            int[] runtimes = new int[] { 0, 6, 12, 18 };
            testAggCoordVar(ncfile, naggs, new DateUnit("hours since 2006-07-29T18:00:00Z"), runtimes);
            testTimeCoordVar(ncfile, naggs, 29, timeVarName, evals);
        }
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
        assert runDim.getLength() == nagg : nagg + " != " + runDim.getLength();
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

        int last = (int) data.getSize() - 1;
        Assert2.assertNearlyEquals(data.getDouble(0), -832.2073364257812);
        Assert2.assertNearlyEquals(data.getDouble(last), 4369.20068359375);
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

            logger.debug(NCdumpW.toString(data));

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

    private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int ntimes, String varName, double[][] timevals)
            throws Exception {
        Variable v = ncfile.findVariable(varName);
        assert v != null : ncfile.getLocation();
        Dimension d = v.getDimension(1); // time dim
        Variable time = ncfile.findVariable(d.getShortName());
        assert null != time;
        logger.debug("time dimension for {} = {}", varName, time.getFullName());

        assert time.getRank() == 2;
        assert time.getSize() == nagg * ntimes;
        assert time.getShape()[0] == nagg;
        assert time.getShape()[1] == ntimes;
        assert time.getDataType() == DataType.DOUBLE || time.getDataType() == DataType.INT;

        String units = time.getUnitsString();
        DateUnit du = new DateUnit(units);

        Array data = time.read();
        logger.debug(NCdumpW.toString(data, "timeCoords", null));

        assert data.getSize() == nagg * ntimes;
        assert data.getShape()[0] == nagg;
        assert data.getShape()[1] == ntimes;
        assert data.getElementType() == double.class || data.getElementType() == int.class;

        DateFormatter formatter = new DateFormatter();
        while (data.hasNext()) {
            double val = data.nextDouble();
            logger.debug("date = {}", Double.isNaN(val) ? val : formatter.toDateTimeStringISO(du.makeDate(val)));
        }

        Index ima = data.getIndex();
        for (int run = 0; run < nagg; run++) {
            for (int tidx = 0; tidx < ntimes; tidx++) {
                double val = data.getDouble(ima.set(run, tidx));
                logger.debug("run={} tidx={} val={}", run, tidx, val);

                if (!Double.isNaN(val)) {
                    Assert2.assertNearlyEquals(val, timevals[run][tidx]);
                }
            }
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
        logger.debug("sum={}", sum);
    }

    private void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {
        Variable v = ncfile.findVariable("P_sfc");

        Array data = v.read(origin, shape);
        assert data.getRank() == 4;
        assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
        assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
        assert data.getShape()[1] == shape[1];
        assert data.getShape()[2] == shape[2];
        assert data.getShape()[3] == shape[3];
        assert data.getElementType() == float.class;

        //Index tIndex = data.getIndex();
        //for (int i = 0; i < shape[0]; i++) {
        //    for (int j = 0; j < shape[1]; j++) {
        //        for (int k = 0; k < shape[2]; k++) {
        //            double val = data.getDouble(tIndex.set(i, j, k));
        //            //logger.debug("{}", val);
        //            assert TestUtils.close(val, 100 * (i + origin[0]) + 10 * j + k) : val;
        //        }
        //    }
        //}
    }

    private void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
        testReadSlice(ncfile, new int[] { 0, 0, 0, 0 }, new int[] { 1, 11, 3, 4 });
        testReadSlice(ncfile, new int[] { 0, 0, 0, 0 }, new int[] { 3, 2, 3, 2 });
        testReadSlice(ncfile, new int[] { 3, 5, 0, 0 }, new int[] { 1, 5, 3, 4 });
        testReadSlice(ncfile, new int[] { 3, 9, 0, 0 }, new int[] { 5, 2, 2, 3 });
    }
}
