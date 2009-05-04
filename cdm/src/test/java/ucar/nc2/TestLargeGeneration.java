package ucar.nc2;

import java.io.IOException;

import ucar.nc2.*;
import ucar.ma2.*;

public class TestLargeGeneration {

  /**
   * @param args
   * @throws InvalidRangeException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, InvalidRangeException {
    String filename = "C:/temp/test3.nc";
    long startAll = System.nanoTime();

    NetcdfFileWriteable writeableFile = null;
    try {
      writeableFile = createLatLonTimeDataCube(filename);
      // The outer dimension has shape 1, since we will write one record
      // at a time, and one
      // record corresponds to one latitude ordinate
      ArrayFloat.D3 variableData = new ArrayFloat.D3(1, LON_LEN, TIME_LEN);
      // ArrayObject.D2 cellIdData = new
      // ArrayObject.D2(class(java.lang.String), latDim.getLength(),
      // lonDim.getLength());
      // An array to record the latitude ordinate
      ucar.ma2.Array latData = ucar.ma2.Array.factory(DataType.FLOAT,
              new int[]{1});

      // The origin to use to write the runoff record
      int[] origin = new int[]{0, 0, 0}; // lat, lon, time
      // The origin to use to write the latitude for each record
      int[] lat_origin = new int[]{0};

//			for (int lat = 0; lat < 177; lat++)
//				latData.setFloat(latData.getIndex(), (float) (lat / 10.0));
      for (int lat = 0; lat < LAT_LEN; lat++) {
        long start = System.nanoTime();
        latData.setFloat(latData.getIndex(), (float) (lat / 10.0));
        for (int lon = 0; lon < LON_LEN; lon++) {
          for (int time = 0; time < TIME_LEN; time++) {
            variableData.set(0, lon, time, (float) time);
          }
        }
        // write the data out for this record
        origin[0] = lat;
        lat_origin[0] = lat;
        writeableFile.write(variableName, origin, variableData);
        writeableFile.write(latVar, lat_origin, latData);
        writeableFile.flush();
        long end = System.nanoTime();
        System.out.printf("write lat= %d time=%f msecs %n", lat, (end-start) / 1000 / 1000.0);
      }
      
    } catch (Throwable t) {
      t.printStackTrace();

    } finally {
      if (writeableFile != null)
        writeableFile.close();
    }

    long endAll = System.nanoTime();
    System.out.printf("total time=%f secs %n", (endAll-startAll) / 1000 / 1000.0/ 1000);

  }

  static String latVar = "lat";
  static String lonVar = "lon";
  static String timeVar = "time";
  static String unitsAttName = "units";
  static String axisAttName = "axis";
  static String standardNameAttName = "standard_name";
  static String longNameAttName = "long_name";
  static float fillValue = -9999.0f;
  static String missingValueAttName = "missing_value";
  static String fillValueAttName = "_FillValue";

  static String variableName = "testVar";
  static int LON_LEN = 300;
  static int LAT_LEN = 300;
  static int TIME_LEN = 40000;
  static String units = "mm";

  private static NetcdfFileWriteable createLatLonTimeDataCube(String filename) throws IOException, InvalidRangeException {
    NetcdfFileWriteable writeableFile = NetcdfFileWriteable.createNew(filename);
    writeableFile.setLargeFile(true);
    writeableFile.setFill(false);
    //writeableFile.setLength((long) 16 * 1000 * 1000 * 1000); // 16 gigs - prealloate

    // define dimensions
    Dimension timeDim = writeableFile.addDimension(timeVar, TIME_LEN);
    Dimension latDim = writeableFile.addUnlimitedDimension(latVar);
    Dimension lonDim = writeableFile.addDimension(lonVar, LON_LEN);

    // define Variables
    Dimension[] dim3 = new Dimension[]{latDim, lonDim, timeDim}; // order matters

    defineHeader(writeableFile, timeDim, latDim, lonDim, dim3);

    // create the file
    writeableFile.create();
    System.out.printf("file=%s%n", writeableFile);
    // write out the non-record variables
    float[] longitudes = new float[LON_LEN];
    int[] times = new int[TIME_LEN];
    for (int i = 0; i < times.length; i++) {
      times[i] = i;
    }
    for (int i = 0; i < longitudes.length; i++) {
      longitudes[i] = (float) i;
    }
    writeableFile.write(lonVar, ucar.ma2.Array.factory(longitudes));
    writeableFile.write(timeVar, ucar.ma2.Array.factory(times));
    return writeableFile;
  }

  private static void defineHeader(NetcdfFileWriteable writeableFile, Dimension timeDim, Dimension latDim, Dimension lonDim, Dimension[] dim3) {
    Dimension[] spatialDim = new Dimension[]{latDim, lonDim};
    writeableFile.addVariable(latVar, DataType.FLOAT, new Dimension[]{latDim});
    writeableFile.addVariableAttribute(latVar, unitsAttName, "degrees_north");
    writeableFile.addVariableAttribute(latVar, axisAttName, "Y");
    writeableFile.addVariableAttribute(latVar, standardNameAttName, "latitude");
    // could add bounds, but not familiar how it works

    writeableFile.addVariable(lonVar, DataType.FLOAT, new Dimension[]{lonDim});
    writeableFile.addVariableAttribute(lonVar, unitsAttName, "degrees_east");
    writeableFile.addVariableAttribute(lonVar, axisAttName, "X");
    writeableFile.addVariableAttribute(lonVar, standardNameAttName, "longitude");
    // could add bounds, but not familiar how it works

    writeableFile.addVariable(variableName, DataType.FLOAT, dim3);
    writeableFile.addVariableAttribute(variableName, longNameAttName, variableName);
    writeableFile.addVariableAttribute(variableName, unitsAttName, units);
    writeableFile.addVariableAttribute(variableName, missingValueAttName, new java.lang.Float(fillValue));
    writeableFile.addVariableAttribute(variableName, fillValueAttName, new java.lang.Float(fillValue));

    writeableFile.addVariable("cellId", DataType.STRING, spatialDim);
    writeableFile.addVariableAttribute("cellId", longNameAttName, "Cell ID");

    writeableFile.addVariable(timeVar, DataType.INT, new Dimension[]{timeDim});
    writeableFile.addVariableAttribute(timeVar, unitsAttName, "days since " + "1889-01-01");
    writeableFile.addVariableAttribute(timeVar, axisAttName, "T");
    writeableFile.addVariableAttribute(timeVar, standardNameAttName, timeVar);
    writeableFile.addVariableAttribute(timeVar, longNameAttName, timeVar);
    // writeableFile.setFill(true);
    // could add bounds, but not familiar how it works
  }

}
