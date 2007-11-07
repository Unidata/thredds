package ucar.nc2;

import junit.framework.*;

import ucar.ma2.*;
import java.io.IOException;

/**
 * Simple example to create a new netCDF file with a "record structure"
 * @author : John Caron
 */
public class TestWriteRecordStructure extends TestCase  {

  static String fileName = TestLocal.cdmTestDataDir +"testWriteRecordStructure.nc"; // default name of file created
  static boolean dumpAfterCreate = false;

  public TestWriteRecordStructure( String name) {
    super(name);
  }

  public void testWriteRecordStructure() throws IOException, InvalidRangeException {
    NetcdfFileWriteable writeableFile = NetcdfFileWriteable.createNew(fileName);

    // define dimensions, including unlimited
    Dimension latDim = writeableFile.addDimension("lat", 3);
    Dimension lonDim = writeableFile.addDimension("lon", 4);
    Dimension timeDim = writeableFile.addUnlimitedDimension("time");

    // define Variables
    Dimension[] dim3 = new Dimension[3];
    dim3[0] = timeDim;
    dim3[1] = latDim;
    dim3[2] = lonDim;

    writeableFile.addVariable("lat", DataType.FLOAT, new Dimension[] {latDim});
    writeableFile.addVariableAttribute("lat", "units", "degrees_north");

    writeableFile.addVariable("lon", DataType.FLOAT, new Dimension[] {lonDim});
    writeableFile.addVariableAttribute("lon", "units", "degrees_east");

    writeableFile.addVariable("rh", DataType.INT, dim3);
    writeableFile.addVariableAttribute("rh", "long_name", "relative humidity");
    writeableFile.addVariableAttribute("rh", "units", "percent");

    writeableFile.addVariable("T", DataType.DOUBLE, dim3);
    writeableFile.addVariableAttribute("T", "long_name", "surface temperature");
    writeableFile.addVariableAttribute("T", "units", "degC");

    writeableFile.addVariable("time", DataType.INT, new Dimension[] {timeDim});
    writeableFile.addVariableAttribute("time", "units", "hours since 1990-01-01");

    // create the file
    writeableFile.create();

    // write out the non-record variables
    writeableFile.write("lat", Array.factory(new float[] {41, 40, 39}));
    writeableFile.write("lon", Array.factory(new float[] {-109, -107, -105, -103}));

    //// heres where we write the record variables

    // different ways to create the data arrays. Note the outer dimension has shape 1.
    ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength());
    ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
    Array timeData = Array.factory( DataType.INT, new int[] {1});

    int[] origin = new int[] {0, 0, 0};
    int[] time_origin = new int[] {0};

    // loop over each record
    for (int time=0; time<10; time++) {
      // make up some data for this record, using different ways to fill the data arrays.
      timeData.setInt(timeData.getIndex(), time * 12);

      Index ima = rhData.getIndex();
      for (int lat=0; lat<latDim.getLength(); lat++)
        for (int lon=0; lon<lonDim.getLength(); lon++) {
          rhData.setInt(ima.set(0, lat, lon), time * lat * lon);
          tempData.set(0, lat, lon, time * lat * lon / 3.14159);
        }

      // write the data out for this record
      time_origin[0] = time;
      origin[0] = time;

      writeableFile.write("rh", origin, rhData);
      writeableFile.write("T", origin, tempData);
      writeableFile.write("time", time_origin, timeData);
    }

    // all done
    writeableFile.close();
  }
}
