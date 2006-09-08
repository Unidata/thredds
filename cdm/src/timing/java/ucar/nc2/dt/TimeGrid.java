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

package ucar.nc2.dt;

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
public class TimeGrid {

    /** testing */
  public static void main( String arg[]) throws IOException {
    //String defaultFilename = "R:/testdata/motherlode/grid/NAM_Alaska_45km_conduit_20060801_0000.grib1";
    String defaultFilename = "R:/testdata/grid/grib/grib2/test/NAM_CONUS_12km_20060305_1200.grib2";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;
    try {
      long start = System.currentTimeMillis();
      GridDataset gridDs = GridDataset.open (filename);
      GeoGrid gg = gridDs.findGridByName("Temperature");
      long took = System.currentTimeMillis() - start;
      System.out.println("open took = " + took+" msecs");

      start = System.currentTimeMillis();
      Array data = gg.readVolumeData(0);
      took = System.currentTimeMillis() - start;
      float size = (float) data.getSize() * gg.getDataType().getSize() / (1000 * 1000);
      System.out.println(size+" Mbytes, took = " + took+" msecs");

    } catch (Exception ioe) {
      ioe.printStackTrace();
    }


    System.in.read();
  }

}
