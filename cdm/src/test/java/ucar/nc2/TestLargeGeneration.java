package ucar.nc2;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NotTravis;

import java.io.File;
import java.io.IOException;

/*
On Travis, this class was causing:
ucar.nc2.TestLargeGeneration > generateLargeFile STANDARD_ERROR
    java.io.IOException: No space left on device
Apparently, writing a 13.4GB file to disk on Travis is a no-no.

In the future, potentially use the more general "Slow" category, because there's likely more places than just
Travis where we don't want to run this.
 */
@Category(NotTravis.class)
public class TestLargeGeneration {

  String latVarName = "lat";
  String lonVarName = "lon";
  String timeVarName = "time";
  String unitsAttName = "units";
  String axisAttName = "axis";
  String standardNameAttName = "standard_name";
  String longNameAttName = CDM.LONG_NAME;
  float fillValue = -9999.0f;
  String missingValueAttName = CDM.MISSING_VALUE;
  String fillValueAttName = "_FillValue";

  String variableName = "testVar";
  int LON_LEN = 300;
  int LAT_LEN = 300;
  int TIME_LEN = 40000;
  String units = "mm";

  @Test
  @Ignore("takes too long")
  public void generateLargeFile() throws IOException, InvalidRangeException {
    File tempFile = File.createTempFile("tmp", "nc", new File(TestDir.temporaryLocalDataDir));
    long startAll = System.nanoTime();

    try (NetcdfFileWriter writer = createLatLonTimeDataCube(tempFile.getPath())) {
      // The outer dimension has shape 1, since we will write one record
      // at a time, and one
      // record corresponds to one latitude ordinate
      ArrayFloat.D3 variableData = new ArrayFloat.D3(1, LON_LEN, TIME_LEN);
      // ArrayObject.D2 cellIdData = new
      // ArrayObject.D2(class(java.lang.String), latDim.getLength(),
      // lonDim.getLength());
      // An array to record the latitude ordinate
      ucar.ma2.Array latData = ucar.ma2.Array.factory(DataType.FLOAT, new int[]{1});

      // The origin to use to write the runoff record
      int[] origin = new int[]{0, 0, 0}; // lat, lon, time
      // The origin to use to write the latitude for each record
      int[] lat_origin = new int[]{0};

//			for (int lat = 0; lat < 177; lat++)
//				latData.setFloat(latData.getIndex(), (float) (lat / 10.0));
      long start = System.nanoTime();
      for (int lat = 0; lat < LAT_LEN; lat++) {
        latData.setFloat(latData.getIndex(), (float) (lat / 10.0));
        for (int lon = 0; lon < LON_LEN; lon++) {
          for (int time = 0; time < TIME_LEN; time++) {
            variableData.set(0, lon, time, (float) time);
          }
        }
        // write the data out for this record
        origin[0] = lat;
        lat_origin[0] = lat;
        writer.write( writer.findVariable(variableName), origin, variableData);
        writer.write( writer.findVariable(latVarName), lat_origin, latData);
        writer.flush();

        if (lat % 10 == 0) {
          long end = System.nanoTime();
          System.out.printf("write lat= %d time=%f msecs %n", lat, (end-start) / 1000 / 1000.0);
          start = System.nanoTime();
        }
      }
      
    } catch (Throwable t) {
      t.printStackTrace();
    }

    long endAll = System.nanoTime();
    System.out.printf("total time=%f secs %n", (endAll-startAll) / 1000 / 1000.0/ 1000);

  }

  private NetcdfFileWriter createLatLonTimeDataCube(String filename) throws IOException, InvalidRangeException {
    NetcdfFileWriter writeableFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);
    writeableFile.setLargeFile(true);
    writeableFile.setFill(false);
    //writeableFile.setLength((long) 16 * 1000 * 1000 * 1000); // 16 gigs - prealloate

    // define dimensions
    Dimension timeDim = writeableFile.addDimension(null, timeVarName, TIME_LEN);
    Dimension latDim = writeableFile.addUnlimitedDimension(latVarName);
    Dimension lonDim = writeableFile.addDimension(null, lonVarName, LON_LEN);

    // define Variables
    defineHeader(writeableFile, timeVarName, latVarName, lonVarName);

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
    writeableFile.write(lonVarName, ucar.ma2.Array.makeFromJavaArray(longitudes, false));
    writeableFile.write(timeVarName, ucar.ma2.Array.makeFromJavaArray(times, false));
    return writeableFile;
  }

  private void defineHeader(NetcdfFileWriter writeableFile, String timeDim, String latDim, String lonDim) {
    Variable latVar = writeableFile.addVariable(latVarName, DataType.FLOAT, latDim);
    writeableFile.addVariableAttribute(latVar, new Attribute( unitsAttName, "degrees_north"));
    writeableFile.addVariableAttribute(latVar, new Attribute(axisAttName, "Y"));
    writeableFile.addVariableAttribute(latVar, new Attribute(standardNameAttName, "latitude"));
    // could add bounds, but not familiar how it works

    Variable lonVar = writeableFile.addVariable(lonVarName, DataType.FLOAT, lonDim);
    writeableFile.addVariableAttribute(lonVar, new Attribute(unitsAttName, "degrees_east"));
    writeableFile.addVariableAttribute(lonVar, new Attribute(axisAttName, "X"));
    writeableFile.addVariableAttribute(lonVar, new Attribute(standardNameAttName, "longitude"));
    // could add bounds, but not familiar how it works

    Variable var = writeableFile.addVariable(variableName, DataType.FLOAT, latDim+" "+lonDim+" "+timeDim);
    writeableFile.addVariableAttribute(var, new Attribute(longNameAttName, variableName));
    writeableFile.addVariableAttribute(var, new Attribute(unitsAttName, units));
    writeableFile.addVariableAttribute(var, new Attribute(missingValueAttName, fillValue));
    writeableFile.addVariableAttribute(var, new Attribute(fillValueAttName, fillValue));

    Variable cellVar = writeableFile.addVariable("cellId", DataType.INT, latDim+" "+lonDim);
    writeableFile.addVariableAttribute(cellVar, new Attribute(longNameAttName, "Cell ID"));

    Variable timeVar = writeableFile.addVariable(timeVarName, DataType.INT, timeDim);
    writeableFile.addVariableAttribute(timeVar, new Attribute(unitsAttName, "days since 1889-01-01"));
    writeableFile.addVariableAttribute(timeVar, new Attribute(axisAttName, "T"));
    writeableFile.addVariableAttribute(timeVar, new Attribute(standardNameAttName, timeVarName));
    writeableFile.addVariableAttribute(timeVar, new Attribute(longNameAttName, timeVarName));
    // writeableFile.setFill(true);
    // could add bounds, but not familiar how it works
  }

}
