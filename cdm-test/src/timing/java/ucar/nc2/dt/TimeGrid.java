/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
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
    //String defaultFilename = "R:/testdata2/motherlode/grid/NAM_Alaska_45km_conduit_20060801_0000.grib1";
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
