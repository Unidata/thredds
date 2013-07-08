package ucar.nc2;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Misc netcdf3 NetcdfFileWriter tests
 *
 * @author caron
 * @since 4/26/12
 */
public class TestWriteMisc {

  /* byte Band1(y, x);
 >     Band1:_Unsigned = "true";
 >     Band1:_FillValue = -1b; // byte
 >
 > byte Band2(y, x);
 >     Band2:_Unsigned = "true";
 >     Band2:valid_range = 0s, 254s; // short
 */

  @Test
  public void testUnsignedAttribute() throws IOException, InvalidRangeException {
     String filename = TestLocal.temporaryDataDir + "testUnsignedAttribute2.nc";
     //String filename = "C:/tmp/testUnsignedAttribute3.nc";
     System.out.printf("%s%n", filename);

     NetcdfFileWriter writer = null;
     try {
       writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);
       writer.addUnlimitedDimension("time");

       //   public Variable addVariable(Group g, String shortName, DataType dataType, String dims) {
       Variable v = writer.addVariable(null, "time", DataType.BYTE, "time");
       writer.addVariableAttribute(v, new Attribute(CDM.UNSIGNED, "true"));
       writer.addVariableAttribute(v, new Attribute(CDM.SCALE_FACTOR, 10.0));
       List<Integer> a = new ArrayList<Integer>();
       a.add(10);
       a.add(240);
       writer.addVariableAttribute(v, new Attribute(CDM.VALID_RANGE, a));

         /* byte Band1(y, x);
 >     Band1:_Unsigned = "true";
 >     Band1:_FillValue = -1b; // byte
 */

       Variable band1 = writer.addVariable(null, "Band1", DataType.BYTE, "time");
       writer.addVariableAttribute(band1, new Attribute(CDM.UNSIGNED, "true"));
       writer.addVariableAttribute(band1, new Attribute(CDM.FILL_VALUE, (byte) -1));
       writer.addVariableAttribute(band1, new Attribute(CDM.SCALE_FACTOR, 1.0));

 /* byte Band2(y, x);
 >     Band2:_Unsigned = "true";
 >     Band2:valid_range = 0s, 254s; // short
 */

       Variable band2 = writer.addVariable(null, "Band2", DataType.BYTE, "time");
       writer.addVariableAttribute(band2, new Attribute(CDM.UNSIGNED, "true"));
       writer.addVariableAttribute(band2, new Attribute(CDM.SCALE_FACTOR, 1.0));
        List<Short> a2 = new ArrayList<Short>();
        a2.add((short)0);
        a2.add((short)254);
        writer.addVariableAttribute(band2, new Attribute(CDM.VALID_RANGE, a2));

       writer.create();

       Array timeData = Array.factory(DataType.BYTE, new int[]{1});
       int[] time_origin = new int[]{0};

       for (int time = 0; time < 256; time++) {
         timeData.setInt(timeData.getIndex(), time);
         time_origin[0] = time;
         writer.write(v, time_origin, timeData);
         writer.write(band1, time_origin, timeData);
         writer.write(band2, time_origin, timeData);
       }

     } catch (IOException ioe) {
       ioe.printStackTrace();
       assert false;

     } finally {
       if (writer != null) writer.close();
     }

     NetcdfFile ncFile = NetcdfFile.open(filename);
     try {
       Array result2 = ncFile.readSection("time");
       System.out.println(result2);
       //ucar.unidata.test.util.CompareNetcdf.compareData(result1, result2);
     } finally {
       ncFile.close();
     }
  }



  // test writing big format
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

    NetcdfFileWriter fileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, TestLocal.temporaryDataDir + "bigFile2.nc");
    fileWriter.setFill(false);
    fileWriter.setLargeFile(true);

    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    fileWriter.setLength(approxSize);

    String timeUnits = "hours since 2008-06-06 12:00:0.0";
    String coordUnits = "degrees";

    Dimension[] dim = new Dimension[3];

    dim[0] = setDimension(fileWriter, "time", timeUnits, timeSize);
    dim[1] = setDimension(fileWriter, "lat", coordUnits, latSize);
    dim[2] = setDimension(fileWriter, "lon", coordUnits, lonSize);

    Variable v = fileWriter.addVariable(null, varName, DataType.FLOAT, Arrays.asList(dim));

    fileWriter.addVariableAttribute(v, new Attribute("_FillValue", -9999));
    fileWriter.addVariableAttribute(v, new Attribute(CDM.MISSING_VALUE, -9999));

    System.out.println("Creating netcdf <=");
    fileWriter.create();

    /////////////////////////////////////
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
        fileWriter.write(v, origin, floatArray);
      }
    }

    fileWriter.close();

    System.out.println("Done <=");
    stop = System.nanoTime();
    took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took " + took + " secs");
    start = stop;
  }

  private static Dimension setDimension(NetcdfFileWriter ncFile, String name, String units, int length) {

    Dimension dimension = ncFile.addDimension(null, name, length);
    Variable v = ncFile.addVariable(null, name, DataType.FLOAT, name);
    ncFile.addVariableAttribute(v, new Attribute("units", units));

    return dimension;
  }

}
