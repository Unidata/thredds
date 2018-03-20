/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Simple example to create a new netCDF file with a "record structure"
 *
 * @author : John Caron
 */
public class TestWriteRecordStructure {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWriteRecordStructure() throws IOException, InvalidRangeException {
    String fileName = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter writeableFile = NetcdfFileWriter.createNew(fileName, true)) {
      // define dimensions, including unlimited
      Dimension latDim = writeableFile.addDimension("lat", 3);
      Dimension lonDim = writeableFile.addDimension("lon", 4);
      Dimension timeDim = writeableFile.addUnlimitedDimension("time");

      // define Variables
      writeableFile.addVariable("lat", DataType.FLOAT, "lat");
      writeableFile.addVariableAttribute("lat", "units", "degrees_north");

      writeableFile.addVariable("lon", DataType.FLOAT, "lon");
      writeableFile.addVariableAttribute("lon", "units", "degrees_east");

      Variable rhVar = writeableFile.addVariable("rh", DataType.INT, "time lat lon");
      writeableFile.addVariableAttribute("rh", CDM.LONG_NAME, "relative humidity");
      writeableFile.addVariableAttribute("rh", "units", "percent");

      Variable tVar = writeableFile.addVariable("T", DataType.DOUBLE, "time lat lon");
      writeableFile.addVariableAttribute("T", CDM.LONG_NAME, "surface temperature");
      writeableFile.addVariableAttribute("T", "units", "degC");

      Variable timeVar = writeableFile.addVariable("time", DataType.INT, "time");
      writeableFile.addVariableAttribute("time", "units", "hours since 1990-01-01");

      // create the file
      writeableFile.create();

      // write out the non-record variables
      writeableFile.write("lat", Array.makeFromJavaArray(new float[]{41, 40, 39}, false));
      writeableFile.write("lon", Array.makeFromJavaArray(new float[]{-109, -107, -105, -103}, false));

      //// heres where we write the record variables

      // different ways to create the data arrays. Note the outer dimension has shape 1.
      ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength(), false);
      ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      Array timeData = Array.factory(DataType.INT, new int[]{1});

      int[] origin = new int[]{0, 0, 0};
      int[] time_origin = new int[]{0};

      // loop over each record
      for (int time = 0; time < 10; time++) {
        // make up some data for this record, using different ways to fill the data arrays.
        timeData.setInt(timeData.getIndex(), time * 12);

        Index ima = rhData.getIndex();
        for (int lat = 0; lat < latDim.getLength(); lat++)
          for (int lon = 0; lon < lonDim.getLength(); lon++) {
            rhData.setInt(ima.set(0, lat, lon), time * lat * lon);
            tempData.set(0, lat, lon, time * lat * lon / 3.14159);
          }

        // write the data out for this record
        time_origin[0] = time;
        origin[0] = time;

        writeableFile.write(rhVar, origin, rhData);
        writeableFile.write(tVar, origin, tempData);
        writeableFile.write(timeVar, time_origin, timeData);
      }
    }
  }
}
