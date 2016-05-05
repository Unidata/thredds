package ucar.nc2;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

public class TestWriteString {

  String variableName = "dataVar";
  String units = "units";

  String latVar = "lat";
  String lonVar = "lon";
  String timeVar = "time";
  String unitsAttName = "units";
  String axisAttName = "axis";
  String standardNameAttName = "standard_name";
  String longNameAttName = CDM.LONG_NAME;
  String missingValueAttName = CDM.MISSING_VALUE;
  String fillValueAttName = "_FillValue";


  private void defineHeader(NetcdfFileWriter writeableFile, String timeDim, String latDim, String lonDim, String dim3) {
    writeableFile.addVariable(latVar, DataType.FLOAT, latDim);
    writeableFile.addVariableAttribute(latVar, unitsAttName, "degrees_north");
    writeableFile.addVariableAttribute(latVar, axisAttName, "Y");
    writeableFile.addVariableAttribute(latVar, standardNameAttName, "latitude");
    // could add bounds, but not familiar how it works

    writeableFile.addVariable(lonVar, DataType.FLOAT, lonDim);
    writeableFile.addVariableAttribute(lonVar, unitsAttName, "degrees_east");
    writeableFile.addVariableAttribute(lonVar, axisAttName, "X");
    writeableFile.addVariableAttribute(lonVar, standardNameAttName, "longitude");
    // could add bounds, but not familiar how it works

    writeableFile.addVariable(variableName, DataType.FLOAT, dim3);
    writeableFile.addVariableAttribute(variableName, longNameAttName, variableName);
    writeableFile.addVariableAttribute(variableName, unitsAttName, units);

    writeableFile.addVariable("cellId", DataType.CHAR, "lat lon"); // STRING  illegal change to CHAR
    writeableFile.addVariableAttribute("cellId", longNameAttName, "Cell ID");

    writeableFile.addVariable(timeVar, DataType.INT, timeDim);
    writeableFile.addVariableAttribute(timeVar, axisAttName, "T");
    writeableFile.addVariableAttribute(timeVar, standardNameAttName, timeVar);
    writeableFile.addVariableAttribute(timeVar, longNameAttName, timeVar);
    writeableFile.setFill(true);
    // could add bounds, but not familiar how it works
  }

  private NetcdfFileWriter createTimeLatLonDataCube(String filename, double[] latitudes, double[] longitudes) throws IOException {
    NetcdfFileWriter writeableFile = NetcdfFileWriter.createNew(filename, true);

    // define dimensions, including unlimited
    Dimension latDim = writeableFile.addDimension(latVar, latitudes.length);
    Dimension lonDim = writeableFile.addDimension(lonVar, longitudes.length);
    Dimension timeDim = writeableFile.addUnlimitedDimension(timeVar);

    // define Variables
    defineHeader(writeableFile, timeVar, latVar, lonVar, timeVar+" "+latVar+" "+lonVar);

    // create the file
    writeableFile.create();
    writeableFile.close();

    return writeableFile;
  }

  // this was succeeding, but it shoulnt - now fails in 4.0.26
  @Test
  public void testWrite() throws IOException {
    TestWriteString test = new TestWriteString();
    File tempFile = File.createTempFile("temp", "nc", new File(TestDir.temporaryLocalDataDir));
    test.createTimeLatLonDataCube(tempFile.getPath(), new double[] {1,2}, new double[] {10,20,30,40});
  }


}
