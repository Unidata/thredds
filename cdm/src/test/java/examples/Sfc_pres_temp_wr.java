package examples;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Dimension;
import ucar.ma2.DataType;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;

import java.util.ArrayList;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Nov 16, 2006
 * Time: 12:11:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sfc_pres_temp_wr {


    public static void main(String args[]) throws Exception
    {
        final int NLAT = 6;
        final int NLON = 12;
        final float SAMPLE_PRESSURE = 900.0f;
        final float SAMPLE_TEMP = 9.0f;
        final float START_LAT = 25.0f;
        final float START_LON = -125.0f;


        // Create the file.
        String filename = "sfc_pres_temp.nc";
        NetcdfFileWriteable dataFile = null;

        try {
            //Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriteable.createNew(filename, false);

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension latDim = dataFile.addDimension("latitude", NLAT, true, false, false);
            Dimension lonDim = dataFile.addDimension("longitude", NLON, true, false, false);
            ArrayList dims =  null;


            dataFile.addVariable("latitude", DataType.FLOAT, new Dimension[] {latDim});
            dataFile.addVariable("longitude", DataType.FLOAT, new Dimension[] {lonDim});

            // Define units attributes for coordinate vars. This attaches a
            // text attribute to each of the coordinate variables, containing
            // the units.

            dataFile.addVariableAttribute("longitude", "units", "degrees_east");
            dataFile.addVariableAttribute("latitude", "units", "degrees_north");

            // Define the netCDF data variables.
            dims =  new ArrayList();
            dims.add(latDim);
            dims.add(lonDim);
            dataFile.addVariable("pressure", DataType.FLOAT, dims);
            dataFile.addVariable("temperature", DataType.FLOAT, dims);

            // Define units attributes for variables.
            dataFile.addVariableAttribute("pressure", "units", "hPa");
            dataFile.addVariableAttribute("temperature", "units", "celsius");

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            try {
                dataFile.create();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }

            ArrayFloat.D1 dataLat = new ArrayFloat.D1(latDim.getLength());
            ArrayFloat.D1 dataLon = new ArrayFloat.D1(lonDim.getLength());

            // Create some pretend data. If this wasn't an example program, we
            // would have some real data to write, for example, model
            // output.
            int i,j;


            for (i=0; i<latDim.getLength(); i++) {
                dataLat.set(i,  START_LAT + 5.f * i );
            }

            for (j=0; j<lonDim.getLength(); j++) {
               dataLon.set(j,  START_LON + 5.f * j );
            }

            try {
                dataFile.write("latitude", dataLat);
                dataFile.write("longitude", dataLon);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            } catch (InvalidRangeException e) {
                e.printStackTrace(System.err);
            }


            // Create the pretend data. This will write our surface pressure and
            // surface temperature data.

            ArrayFloat.D2 dataTemp = new ArrayFloat.D2(latDim.getLength(), lonDim.getLength());
            ArrayFloat.D2 dataPres = new ArrayFloat.D2(latDim.getLength(), lonDim.getLength());

            for (i=0; i<latDim.getLength(); i++) {
                for (j=0; j<lonDim.getLength(); j++) {
                   dataTemp.set(i,j,  SAMPLE_TEMP + .25f * (j * NLAT + i));
                   dataPres.set(i,j,  SAMPLE_PRESSURE + (j * NLAT + i));
                }
            }

            int[] origin = new int[2];
            try {
              dataFile.write("pressure", origin, dataPres);
              dataFile.write("temperature", origin, dataTemp);
            } catch (IOException e) {
              System.err.println("ERROR writing file");
            } catch (InvalidRangeException e) {
              e.printStackTrace();
            }

        // The file is closed.
        } finally {
            if (null != dataFile)
            try {
                dataFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
       }
       System.out.println( "*** SUCCESS writing example file sfc_pres_temp.nc!" );
    }
}

