package ucar.nc2;

import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Jun 11, 2009
 */
public class TestWriteString {

  String variableName = "dataVar";
  String units = "units";

  String latVar = "lat";
  String lonVar = "lon";
  String timeVar = "time";
  String unitsAttName = "units";
  String axisAttName = "axis";
  String standardNameAttName = "standard_name";
  String longNameAttName = "long_name";
  String missingValueAttName = "missing_value";
  String fillValueAttName = "_FillValue";


  private void defineHeader(NetcdfFileWriteable writeableFile, Dimension timeDim, Dimension latDim, Dimension lonDim, Dimension[] dim3) {
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

    writeableFile.addVariable("cellId", DataType.STRING, spatialDim); // now illegal
    writeableFile.addVariableAttribute("cellId", longNameAttName, "Cell ID");

    writeableFile.addVariable(timeVar, DataType.INT, new Dimension[]{timeDim});
    writeableFile.addVariableAttribute(timeVar, axisAttName, "T");
    writeableFile.addVariableAttribute(timeVar, standardNameAttName, timeVar);
    writeableFile.addVariableAttribute(timeVar, longNameAttName, timeVar);
    writeableFile.setFill(true);
    // could add bounds, but not familiar how it works
  }

  private NetcdfFileWriteable createTimeLatLonDataCube(String filename, double[] latitudes, double[] longitudes) throws IOException {
    NetcdfFileWriteable writeableFile = NetcdfFileWriteable.createNew(filename);

    // define dimensions, including unlimited
    Dimension latDim = writeableFile.addDimension(latVar, latitudes.length);
    Dimension lonDim = writeableFile.addDimension(lonVar, longitudes.length);
    Dimension timeDim = writeableFile.addUnlimitedDimension(timeVar);

    // define Variables
    Dimension[] dim3 = new Dimension[]{timeDim, latDim, lonDim}; // order matters

    defineHeader(writeableFile, timeDim, latDim, lonDim, dim3);

    // create the file
    writeableFile.create();

    writeableFile.close();

    return  writeableFile;
  }

  // this was succeeding, but it shoulnt - now fails in 4.0.26
  public static void main(String[] args) throws IOException {
    TestWriteString test = new TestWriteString();
    test.createTimeLatLonDataCube("D:/work/csiro/testWrite.nc", new double[] {1,2}, new double[] {10,20,30,40});
  }


}
