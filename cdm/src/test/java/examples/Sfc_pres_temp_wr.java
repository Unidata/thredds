/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package examples;

/* This is part of the netCDF package.
 Copyright 2006 University Corporation for Atmospheric Research/Unidata.
 See COPYRIGHT file for conditions of use.

 This example writes some surface pressure and temperatures. It is
 intended to illustrate the use of the netCDF Java API. The companion
 program sfc_pres_temp_rd.java shows how to read the netCDF data file
 created by this program.

 This example demonstrates the netCDF Java API.

 Full documentation of the netCDF Java API can be found at:
 http://www.unidata.ucar.edu/software/netcdf-java/
 */

/*
 Example contributed by Peter Jansen
 */

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class Sfc_pres_temp_wr
{

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
        NetcdfFileWriter dataFile = null;

        try
        {
            // Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension latDim = dataFile.addDimension(null, "latitude", NLAT);
            Dimension lonDim = dataFile.addDimension(null, "longitude", NLON);
            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(latDim);
            dims.add(lonDim);

            Variable vlat = dataFile.addVariable(null, "latitude", DataType.FLOAT, "latitude");
            Variable vlon = dataFile.addVariable(null, "longitude", DataType.FLOAT, "longitude");

            Variable vPres = dataFile.addVariable(null, "pressure", DataType.FLOAT, dims);
            Variable vTemp = dataFile.addVariable(null, "temperature", DataType.FLOAT, dims);

            // Define units attributes for coordinate vars. This attaches a
            // text attribute to each of the coordinate variables, containing
            // the units.

            vlat.addAttribute(new Attribute("units", "degrees_east"));
            vlon.addAttribute(new Attribute("units", "degrees_north"));

            // Define units attributes for variables.
            vPres.addAttribute(new Attribute("units", "hPa"));
            vTemp.addAttribute(new Attribute("units", "celsius"));

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            dataFile.create();

            Array dataLat = Array.factory(DataType.FLOAT, new int[] { NLAT });
            Array dataLon = Array.factory(DataType.FLOAT, new int[] { NLON });

            // Create some pretend data. If this wasn't an example program, we
            // would have some real data to write, for example, model
            // output.
            int i, j;

            for (i = 0; i < latDim.getLength(); i++)
            {
                dataLat.setFloat(i, START_LAT + 5.f * i);
            }

            for (j = 0; j < lonDim.getLength(); j++)
            {
                dataLon.setFloat(j, START_LON + 5.f * j);
            }

            dataFile.write(vlat, dataLat);
            dataFile.write(vlon, dataLon);

            // Create the pretend data. This will write our surface pressure and
            // surface temperature data.

            int[] iDim = new int[] { latDim.getLength(), lonDim.getLength() };
            Array dataTemp = Array.factory(DataType.FLOAT, iDim);
            Array dataPres = Array.factory(DataType.FLOAT, iDim);

            Index2D idx = new Index2D(iDim);

            for (i = 0; i < latDim.getLength(); i++)
            {
                for (j = 0; j < lonDim.getLength(); j++)
                {
                    idx.set(i, j);
                    dataTemp.setFloat(idx, SAMPLE_TEMP + .25f * (j * NLAT + i));
                    dataPres.setFloat(idx, SAMPLE_PRESSURE + (j * NLAT + i));
                }
            }

            dataFile.write(vPres, dataPres);
            dataFile.write(vTemp, dataTemp);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InvalidRangeException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != dataFile)
            {
                try
                {
                    dataFile.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        }
        System.out.println("*** SUCCESS writing example file sfc_pres_temp.nc!");
    }
}

