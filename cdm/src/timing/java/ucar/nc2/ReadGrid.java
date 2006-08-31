// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2;

import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class ReadGrid {

  public static void main( String arg[]) throws IOException {
    String defaultFilename = "C:/data/grib/nam/conus12/NAM_CONUS_12km_20060604_1800.grib2";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;

    GridDataset gds = GridDataset.open (filename);
    GeoGrid grid = gds.findGridByName("Temperature");

    long startTime = System.currentTimeMillis();

    Array data = grid.readDataSlice(0, -1, -1, -1);

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    System.out.println("read "+data.getSize()+"  took "+diff+ " msecs");

    startTime = endTime;
    float[] jdata = (float []) data.get1DJavaArray(float.class);
    endTime = System.currentTimeMillis();
    diff = endTime - startTime;
    System.out.println("convert took "+diff+ " msecs "+jdata[0]);


  }

}
