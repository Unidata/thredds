/* Copyright Unidata */
package ucar.nc2.grib.grib1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.grib.GribData;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test misc GRIB1 unpacking
 *
 * @author caron
 * @since 11/23/2015.
 */
@RunWith(JUnit4.class)
public class TestGrib1Unpack {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing.grib1";
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {
      Variable var = nc.findVariable("Minimum_temperature_at_2_metres_in_the_last_6_hours_surface_6_Hour");
      Array data = var.read();
      float first = data.getFloat(0);

      Assert.assertEquals(264.135559, first, 1e-6);
    }
  }

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData2() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing2.grib1";
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {

      Variable var = nc.findVariable("Snowfall_surface");
      Array data = var.read();

      float first = data.getFloat(0);

      Assert.assertEquals(.326607, first, 1e-6);
    }
  }

  // Tests reading a thin grid record thats at the the end of the file
  // sample file has single record
  @Test
  public void testThinGridAtEndofFile() throws IOException {
    final String testfile = "../grib/src/test/data/thinGrid.grib1";
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {

      Variable var = nc.findVariable("Temperature_isobaric");
      Array data = var.read();
      Assert.assertEquals(73*73, data.getSize());

      float first = data.getFloat(0);
      float last = data.getFloat((73*73)-1);

      Assert.assertEquals(291.0, first, 1e-6);
      Assert.assertEquals(278.0, last, 1e-6);
    }
  }

  // https://github.com/Unidata/thredds/issues/82
  // not sure what it should be
  // raw:  line 22: 96057.882813,96302.679688,96524.906250,96693.546875,96893.937500,97179.359375,97464.890625,97647.703125,97722.148438,97733.218750,97721.312500,97769.500000,98008.898438,98483.460938,99064.515625,99570.500000,99935.101563,100199.570313,100381.640625,100439.078125,100336.703125,100103.062500,99817.539063,99564.757813,99389.359375,99292.281250,99253.007813,99231.726563,99176.851563,99073.953125,98956.421875,98831.359375,98658.148438,98440.015625,98264.398438,
  // linear:
  // cubic:
  @Test
  public void testThisGridInterpolation() throws IOException {
    final String testfile = "../grib/src/test/data/HPPI89_KWBC.grib1";

    Grib1Index index = new Grib1Index();
    if (!index.readIndex(testfile, -1)) {
      index.makeIndex(testfile, null);
    }

    int lineno = 0;
    try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(testfile, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);

      for (Grib1Record gr : index.getRecords()) {
        getData(raf, gr, GribData.InterpolationMethod.none, lineno);
        getData(raf, gr, GribData.InterpolationMethod.linear, lineno);
        getData(raf, gr, GribData.InterpolationMethod.cubic, lineno);
      }
    }
  }

  private float[] getData(ucar.unidata.io.RandomAccessFile raf, Grib1Record gr, GribData.InterpolationMethod method, int lineno) throws IOException {
    float[] data;
    data = gr.readData(raf, method);

    System.out.printf("%s%n", method);
    Grib1Gds gds = gr.getGDS();
    if (method == GribData.InterpolationMethod.none) {
      int[] lines = gds.getNptsInLine();
      int count = 0;
      for (int line=0; line<lines.length; line++) {
        if (line != lineno) continue;
        System.out.printf(" %3d: ", line);
        for (int i=0; i<lines[line]; i++) System.out.printf("%f,", data[count++]);
        System.out.printf("%n");
      }

    } else {
      int[] shape = new int[]{gds.getNy(), gds.getNx()};
      Array dataA = Array.factory(DataType.FLOAT, shape, data);
      Array lineA = dataA.slice(0, lineno);
      logger.debug("{}", NCdumpW.toString(lineA));
    }
    System.out.printf("%s%n", method);

    return data;
  }
}
