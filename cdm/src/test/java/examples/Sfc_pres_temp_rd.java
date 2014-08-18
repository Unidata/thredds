/* This is part of the netCDF package.
   Copyright 2006 University Corporation for Atmospheric Research/Unidata.
 *
    See COPYRIGHT file for conditions of use.

   This is an example which reads some surface pressure and
   temperatures. The data file read by this program is produced
   companion program sfc_pres_temp_wr.java. It is intended to
   illustrate the use of the netCDF Java API.

   This example demonstrates the netCDF Java API.

   Full documentation of the netCDF Java API can be found at:
   http://www.unidata.ucar.edu/software/netcdf-java/
 */
package examples;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.ArrayFloat;

import java.io.IOException;

/**
 * User: yuanho
 * Date: Nov 16, 2006
 * Time: 11:59:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class Sfc_pres_temp_rd {


  public static void main(String args[]) throws Exception {
    final int NLAT = 6;
    final int NLON = 12;

    // These are used to calculate the values we expect to find.
    final float SAMPLE_PRESSURE = 900f;
    final float SAMPLE_TEMP = 9.0f;
    final float START_LAT = 25.0f;
    final float START_LON = -125.0f;

    // These will hold our pressure and temperature data.
    float[][] presIn = new float[NLAT][NLON];
    float[][] tempIn = new float[NLAT][NLON];

    // These will hold our latitudes and longitudes.
    float[] latsIn = new float[NLAT];
    float[] lonsIn = new float[NLON];


    // Open the file and check to make sure it's valid.
    String filename = "sfc_pres_temp.nc";
    NetcdfFile dataFile = null;

    try {
      dataFile = NetcdfFile.open(filename, null);

      Variable latVar = dataFile.findVariable("latitude");
      if (latVar == null) {
        System.out.println("Cant find Variable latitude");
        return;
      }


      Variable lonVar = dataFile.findVariable("longitude");
      if (lonVar == null) {
        System.out.println("Cant find Variable longitude");
        return;
      }


      Variable presVar = dataFile.findVariable("pressure");
      if (presVar == null) {
        System.out.println("Cant find Variable pressure");
        return;
      }

      Variable tempVar = dataFile.findVariable("temperature");
      if (tempVar == null) {
        System.out.println("Cant find Variable temperature");
        return;
      }


      if (latVar.getDimensions().size() != 1) {
        System.out.println(" fail to get the dimensions of variable latitude");
        return;
      }
      if (presVar.getDimensions().size() != 2) {
        System.out.println(" fail to get the dimensions of variable pressure");
        return;
      }

      // Read the latitude and longitude coordinate variables into arrays
      // latsIn and lonsIn.

      ArrayFloat.D1 latArray;
      ArrayFloat.D1 lonArray;

      latArray = (ArrayFloat.D1) latVar.read();
      lonArray = (ArrayFloat.D1) lonVar.read();


      int[] shape = latArray.getShape();
      for (int i = 0; i < shape[0]; i++) {
        latsIn[i] = latArray.get(i);
      }

      shape = lonArray.getShape();
      for (int j = 0; j < shape[0]; j++) {
        lonsIn[j] = lonArray.get(j);
      }

      // Check the coordinate variable data.
      for (int lat = 0; lat < NLAT; lat++)
        if (latsIn[lat] != START_LAT + 5. * lat)
          System.err.println("ERROR reading variable latitude");

      // Check longitude values.
      for (int lon = 0; lon < NLON; lon++)
        if (lonsIn[lon] != START_LON + 5. * lon)
          System.err.println("ERROR reading variable longitude");


      // Read the data. Since we know the contents of the file we know
      // that the data arrays in this program are the correct size to
      // hold all the data.
      ArrayFloat.D2 presArray, tempArray;

      presArray = (ArrayFloat.D2) presVar.read();
      tempArray = (ArrayFloat.D2) tempVar.read();

      int[] shape1 = presArray.getShape();

      for (int i = 0; i < shape1[0]; i++) {
        for (int j = 0; j < shape1[1]; j++) {
          presIn[i][j] = presArray.get(i, j);
          tempIn[i][j] = tempArray.get(i, j);
        }
      }


      // Check the data.
      for (int lat = 0; lat < NLAT; lat++)
        for (int lon = 0; lon < NLON; lon++)
          if (presIn[lat][lon] != SAMPLE_PRESSURE + (lon * NLAT + lat)
                  || tempIn[lat][lon] != SAMPLE_TEMP + .25 * (lon * NLAT + lat))
            System.err.println("ERROR reading variable pressure or temperature");

      // Each of the netCDF variables has a "units" attribute. Let's read
      // them and check them.

      if (!latVar.findAttributeIgnoreCase("units").getStringValue().equalsIgnoreCase("degrees_north"))
        System.err.println("ERROR reading variable latitude units");

      if (!lonVar.findAttributeIgnoreCase("units").getStringValue().equalsIgnoreCase("degrees_east"))
        System.err.println("ERROR reading variable longitude units");

      if (!presVar.findAttributeIgnoreCase("units").getStringValue().equalsIgnoreCase("hPa"))
        System.err.println("ERROR reading variable pressure units");

      if (!tempVar.findAttributeIgnoreCase("units").getStringValue().equalsIgnoreCase("celsius"))
        System.err.println("ERROR reading variable temperature units");


    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
    } finally {
      if (dataFile != null)
        try {
          dataFile.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
    }
    System.out.println("*** SUCCESS reading example file sfc_pres_temp.nc!");
  }
}
