
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
 * Time: 11:17:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class Pres_temp_4D_wr {

    public static void main(String args[]) throws Exception
    {

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
        NetcdfFileWriteable dataFile = null;

        try {
            // Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriteable.createNew(filename, false);

            //add dimensions  where time dimension is unlimit
            Dimension lvlDim = dataFile.addDimension("level", NLVL ); //, true, false, false);
            Dimension latDim = dataFile.addDimension("latitude", NLAT ); //, true, false, false);
            Dimension lonDim = dataFile.addDimension("longitude", NLON ); //, true, false, false);
            Dimension timeDim = dataFile.addUnlimitedDimension("time");

            ArrayList dims =  null;

            // Define the coordinate variables.
            dataFile.addVariable("latitude", DataType.FLOAT, new Dimension[] {latDim});
            dataFile.addVariable("longitude", DataType.FLOAT, new Dimension[] {lonDim});

            // Define units attributes for data variables.
            dataFile.addVariableAttribute("latitude", "units", "degrees_north");
            dataFile.addVariableAttribute("longitude", "units", "degrees_east");

            // Define the netCDF variables for the pressure and temperature
            // data.
            dims =  new ArrayList();
            dims.add(timeDim);
            dims.add(lvlDim);
            dims.add(latDim);
            dims.add(lonDim);
            dataFile.addVariable("pressure", DataType.FLOAT, dims);
            dataFile.addVariable("temperature", DataType.FLOAT, dims);

            // Define units attributes for data variables.
            dataFile.addVariableAttribute("pressure", "units", "hPa");
            dataFile.addVariableAttribute("temperature", "units", "celsius");




            // Create some pretend data. If this wasn't an example program, we
            // would have some real data to write for example, model output.
            ArrayFloat.D1 lats = new ArrayFloat.D1(latDim.getLength());
            ArrayFloat.D1 lons = new ArrayFloat.D1(lonDim.getLength());
            int i,j;

            for (i=0; i<latDim.getLength(); i++) {
                lats.set(i,  START_LAT + 5.f * i );
            }

            for (j=0; j<lonDim.getLength(); j++) {
               lons.set(j,   START_LON + 5.f * j);
            }


            // Create the pretend data. This will write our surface pressure and
            // surface temperature data.
            ArrayFloat.D4 dataTemp = new ArrayFloat.D4(NREC,lvlDim.getLength(),latDim.getLength(), lonDim.getLength());
            ArrayFloat.D4 dataPres = new ArrayFloat.D4(NREC,lvlDim.getLength(),latDim.getLength(), lonDim.getLength());

            for(int record = 0; record < NREC; record++) {
               i = 0;
               for (int lvl = 0; lvl < NLVL; lvl++)
                  for (int lat = 0; lat < NLAT; lat++)
                    for (int lon = 0; lon < NLON; lon++)
                    {
                        dataPres.set(record, lvl, lat, lon, SAMPLE_PRESSURE + i);
                        dataTemp.set(record, lvl, lat, lon, SAMPLE_TEMP + i++);
                    }
            }

            //Create the file. At this point the (empty) file will be written to disk
            dataFile.create();

            // A newly created Java integer array to be initialized to zeros.
            int[] origin = new int[4];

            dataFile.write("latitude", lats);
            dataFile.write("longitude", lons);
            dataFile.write("pressure", origin, dataPres);
            dataFile.write("temperature", origin, dataTemp);


        } catch (IOException e) {
                e.printStackTrace(System.err);
        } catch (InvalidRangeException e) {
                e.printStackTrace(System.err);
        }finally {
            if (dataFile != null)
            try {
             dataFile.close();
            } catch (IOException ioe) {
             ioe.printStackTrace();
            }
        }
        System.out.println("*** SUCCESS writing example file "+filename);
    }

}