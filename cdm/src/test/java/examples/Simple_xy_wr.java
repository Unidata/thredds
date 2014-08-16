/* This is part of the netCDF package.
   Copyright 2006 University Corporation for Atmospheric Research/Unidata.
   See COPYRIGHT file for conditions of use.

   This is a very simple example which writes a 2D array of
   sample data. To handle this in netCDF we create two shared
   dimensions, "x" and "y", and a netCDF variable, called "data".

   This example demonstrates the netCDF Java API.

   Full documentation of the netCDF Java API can be found at:
   http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/documentation.htm
*/

package examples;

import ucar.nc2.Dimension;
import ucar.ma2.*;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Simple_xy_wr {

  public static void main(String args[]) {
    // We are writing 2D data, a 6 x 12 grid.
    final int NX = 6;
    final int NY = 12;

    String filename = "simple_xy.nc";
    NetcdfFileWriter dataFile = null;

    try {
      dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);

      // Create netCDF dimensions,
      Dimension xDim = dataFile.addDimension(null, "x", NX);
      Dimension yDim = dataFile.addDimension(null, "y", NY);

      // define dimensions
      List<Dimension> dims = new ArrayList<>();
      dims.add(xDim);
      dims.add(yDim);

      // Define a netCDF variable. The type of the variable in this case
      // is ncInt (32-bit integer).
      Variable dataVariable = dataFile.addVariable(null, "data", DataType.INT, dims);

      // create the file
      dataFile.create();

      // This is the data array we will write. It will just be filled
      // with a progression of numbers for this example.
      ArrayInt.D2 dataOut = new ArrayInt.D2(xDim.getLength(), yDim.getLength());

      // Create some pretend data. If this wasn't an example program, we
      // would have some real data to write, for example, model output.
      int i, j;

      for (i = 0; i < xDim.getLength(); i++) {
        for (j = 0; j < yDim.getLength(); j++) {
          dataOut.set(i, j, i * NY + j);
        }
      }

      // Write the pretend data to the file. Although netCDF supports
      // reading and writing subsets of data, in this case we write all
      // the data in one operation.
      dataFile.write(dataVariable, dataOut);

    } catch (IOException e) {
      e.printStackTrace();

    } catch (InvalidRangeException e) {
      e.printStackTrace();

    } finally {
      if (null != dataFile)
        try {
          dataFile.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
    }

    System.out.println("*** SUCCESS writing example file simple_xy.nc!");
  }

}
