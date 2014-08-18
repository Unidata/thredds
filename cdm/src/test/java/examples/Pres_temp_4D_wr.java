/* This is part of the netCDF package.
   Copyright 2006 University Corporation for Atmospheric Research/Unidata.
   See COPYRIGHT file for conditions of use.

   This is an example program which writes some 4D pressure and
   temperatures. It is intended to illustrate the use of the netCDF
   Java API. The companion program pres_temp_4D_rd.java shows how
   to read the netCDF data file created by this program.

   This example demonstrates the netCDF Java API.

   Full documentation of the netCDF Java API can be found at:
   http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/documentation.htm
*/
package examples;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.ma2.DataType;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;

public class Pres_temp_4D_wr {

  public static void main(String args[]) throws Exception {

    final int NLVL = 2;
    final int NLAT = 6;
    final int NLON = 12;
    final int NREC = 2;

    final float SAMPLE_PRESSURE = 900.0f;
    final float SAMPLE_TEMP = 9.0f;
    final float START_LAT = 25.0f;
    final float START_LON = -125.0f;

    // Create the file.
    String filename = "pres_temp_4D.nc";
    NetcdfFileWriter dataFile = null;

    try {
      // Create new netcdf-3 file with the given filename
      dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);

      //add dimensions  where time dimension is unlimit
      Dimension lvlDim = dataFile.addDimension(null, "level", NLVL);
      Dimension latDim = dataFile.addDimension(null, "latitude", NLAT);
      Dimension lonDim = dataFile.addDimension(null, "longitude", NLON);
      Dimension timeDim = dataFile.addUnlimitedDimension("time");

      // Define the coordinate variables.
      Variable latVar = dataFile.addVariable(null, "latitude", DataType.FLOAT, "latitude");
      Variable lonVar = dataFile.addVariable(null, "longitude", DataType.FLOAT, "longitude");

      // Define units attributes for data variables.
      dataFile.addVariableAttribute(latVar, new Attribute("units", "degrees_north"));
      dataFile.addVariableAttribute(lonVar, new Attribute("units", "degrees_east"));

      // Define the netCDF variables for the pressure and temperature
      // data.
      String dims = "time level latitude longitude";
      Variable presVar = dataFile.addVariable(null, "pressure", DataType.FLOAT, dims);
      Variable tempVar = dataFile.addVariable(null, "temperature", DataType.FLOAT, dims);

      // Define units attributes for data variables.
      dataFile.addVariableAttribute(presVar, new Attribute("units", "hPa"));
      dataFile.addVariableAttribute(tempVar, new Attribute("units", "celsius"));

      // Create some pretend data. If this wasn't an example program, we
      // would have some real data to write for example, model output.
      ArrayFloat.D1 lats = new ArrayFloat.D1(latDim.getLength());
      ArrayFloat.D1 lons = new ArrayFloat.D1(lonDim.getLength());
      int i, j;

      for (i = 0; i < latDim.getLength(); i++) {
        lats.set(i, START_LAT + 5.f * i);
      }

      for (j = 0; j < lonDim.getLength(); j++) {
        lons.set(j, START_LON + 5.f * j);
      }

      // Create the pretend data. This will write our surface pressure and
      // surface temperature data.
      ArrayFloat.D4 dataTemp = new ArrayFloat.D4(NREC, lvlDim.getLength(), latDim.getLength(), lonDim.getLength());
      ArrayFloat.D4 dataPres = new ArrayFloat.D4(NREC, lvlDim.getLength(), latDim.getLength(), lonDim.getLength());

      for (int record = 0; record < NREC; record++) {
        i = 0;
        for (int lvl = 0; lvl < NLVL; lvl++)
          for (int lat = 0; lat < NLAT; lat++)
            for (int lon = 0; lon < NLON; lon++) {
              dataPres.set(record, lvl, lat, lon, SAMPLE_PRESSURE + i);
              dataTemp.set(record, lvl, lat, lon, SAMPLE_TEMP + i++);
            }
      }

      //Create the file. At this point the (empty) file will be written to disk
      dataFile.create();

      // A newly created Java integer array to be initialized to zeros.
      int[] origin = new int[4];

      dataFile.write(latVar, lats);
      dataFile.write(lonVar, lons);
      dataFile.write(presVar, origin, dataPres);
      dataFile.write(tempVar, origin, dataTemp);


    } catch (IOException e) {
      e.printStackTrace(System.err);

    } catch (InvalidRangeException e) {
      e.printStackTrace(System.err);

    } finally {
      if (dataFile != null)
        try {
          dataFile.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
    }
    System.out.println("*** SUCCESS writing example file " + filename);
  }

}