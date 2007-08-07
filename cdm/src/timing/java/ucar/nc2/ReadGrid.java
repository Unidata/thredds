package ucar.nc2;

import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
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
