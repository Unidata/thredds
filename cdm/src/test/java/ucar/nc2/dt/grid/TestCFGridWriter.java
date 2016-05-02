package ucar.nc2.dt.grid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lesserwhirls on 7/28/14.
 */
public class TestCFGridWriter {

    GridDataset gds = null;

    @Before
    public void readDataset() {
        String fileIn = TestDir.cdmLocalTestDataDir + "testCFGridWriter.nc4";
        try {
            gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);
        } catch (Exception exc) {
            exc.printStackTrace();
        };
    }

  @Test
   public void testNullLatLonRect() {
       String location = TestDir.temporaryLocalDataDir + "nullLatLonBB.nc";

       boolean addLatLon = false;
       LatLonRect llbb = null;
       Range zRange = null;
       CalendarDateRange dateRange = null;
       List<String> gridList = new ArrayList<String>();
       gridList.add("Temperature_surface");
       int stride_time = 1;
       int horizStride = 1;
       try {
           CFGridWriter writer = new CFGridWriter();
           writer.makeFile(location, gds, gridList,
                   llbb, horizStride,
                   zRange,
                   dateRange, stride_time,
                   addLatLon);
           assert true;
       } catch (Exception exc) {
           exc.printStackTrace();
           assert false;
       }
   }

  @Test
  public void testNullHorizSubset2() throws IOException, InvalidRangeException {
    String location = TestDir.temporaryLocalDataDir + "nullLatLonBB.nc";

    boolean addLatLon = false;
    LatLonRect llbb = null;
    Range zRange = null;
    CalendarDateRange dateRange = null;
    List<String> gridList = new ArrayList<>();
    gridList.add("Temperature_surface");
    int stride_time = 1;
    int horizStride = 1;

    File outFile = new File(TestDir.temporaryLocalDataDir, "testNullLatLonRect2.nc");
    if (outFile.exists())
      outFile.delete();

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outFile.getAbsolutePath());
    CFGridWriter2.writeFile(gds, gridList,
            null, null, horizStride,
            zRange,
            dateRange, stride_time,
            addLatLon,
            writer);

    assert outFile.exists();
    try ( GridDataset result = ucar.nc2.dt.grid.GridDataset.open(outFile.getAbsolutePath())) {
      System.out.printf("result = %s%n", result.getLocationURI());
    }
  }

     @Test
    public void testNullProjectionRect() {
        String location = TestDir.temporaryLocalDataDir + "nullLatLonBB.nc";

        boolean addLatLon = false;
        ProjectionRect llbb = null;
        Range zRange = null;
        CalendarDateRange dateRange = null;
        List<String> gridList = new ArrayList<String>();
        gridList.add("Temperature_surface");
        int stride_time = 1;
        int horizStride = 1;
        try {
            CFGridWriter writer = new CFGridWriter();
            writer.makeFile(location, gds, gridList,
                    llbb, horizStride,
                    zRange,
                    dateRange, stride_time,
                    addLatLon);
            assert true;
        } catch (Exception exc) {
            exc.printStackTrace();
            assert false;
        }
    }


    @After
    public void closeDataset() {
        try {
            gds.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
