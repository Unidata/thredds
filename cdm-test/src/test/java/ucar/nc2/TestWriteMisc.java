package ucar.nc2;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 4/26/12
 */
public class TestWriteMisc {

  @Test
  public void testBig() throws IOException, InvalidRangeException {

    long start = System.nanoTime();
    System.out.println("Begin <=");

    String varName = "example";

    int timeSize = 8;
    int latSize = 8022;
    int lonSize = 10627;

    System.out.println("File size  (B)  = " + (long) timeSize * latSize * lonSize * 4);
    System.out.println("File size~ (MB) = " + Math.round((long) timeSize * latSize * lonSize * 4 / Math.pow(2, 20)));

    NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(TestLocal.temporaryDataDir + "bigFile2.nc");
    ncFile.setFill(false);
    ncFile.setLargeFile(true);

    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    ncFile.setLength(approxSize);

    String timeUnits = "hours since 2008-06-06 12:00:0.0";
    String coordUnits = "degrees";

    Dimension[] dim = new Dimension[3];

    dim[0] = setDimension(ncFile, "time", timeUnits, timeSize);
    dim[1] = setDimension(ncFile, "lat", coordUnits, latSize);
    dim[2] = setDimension(ncFile, "lon", coordUnits, lonSize);

    ncFile.addVariable(varName, DataType.FLOAT, dim);

    ncFile.addVariableAttribute(varName, "_FillValue", -9999);
    ncFile.addVariableAttribute(varName, CDM.MISSING_VALUE, -9999);

    System.out.println("Creating netcdf <=");
    ncFile.create();
    long stop = System.nanoTime();
    double took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took " + took + " secs");
    start = stop;

    System.out.println("Writing netcdf <=");

    int[] shape = new int[]{1, 1, lonSize};
    float[] floatStorage = new float[lonSize];
    Array floatArray = Array.factory(float.class, shape, floatStorage);
    for (int t = 0; t < timeSize; t++) {
      for (int i = 0; i < latSize; i++) {
        int[] origin = new int[]{t, i, 0};
        ncFile.write(varName, origin, floatArray);
      }
    }

    ncFile.close();

    System.out.println("Done <=");
    stop = System.nanoTime();
    took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took " + took + " secs");
    start = stop;
  }

  private static Dimension setDimension(NetcdfFileWriteable ncFile, String name, String units, int length) {

    Dimension dimension = ncFile.addDimension(name, length);
    ncFile.addVariable(name, DataType.FLOAT, new Dimension[]{dimension});
    ncFile.addVariableAttribute(name, "units", units);

    return dimension;
  }

}
