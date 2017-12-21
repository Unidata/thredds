package ucar.nc2.dt.grid;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.util.test.TestDir;

/**
 * Created by lesserwhirls on 7/28/14.
 */
public class TestCFGridWriter2 {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
    GridDataset gds = null;

    @Before
    public void readDataset() {
        String fileIn = TestDir.cdmLocalTestDataDir + "testCFGridWriter.nc4";
        try {
            gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

  @Test
   public void testNullLatLonRect() throws IOException {
       String location = tempFolder.newFile().getAbsolutePath();

       List<String> gridList = new ArrayList<>();
       gridList.add("Temperature_surface");
       int stride_time = 1;
       int horizStride = 1;
       try {
         NetcdfFileWriter ncwriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location);
         CFGridWriter2.writeFile(gds, gridList, null, null, horizStride, null, null, stride_time, false, ncwriter);
         assert true;
       } catch (Exception exc) {
           exc.printStackTrace();
           assert false;
       }
   }

  @Test
  public void testNullHorizSubset2() throws IOException, InvalidRangeException {
    boolean addLatLon = false;
    LatLonRect llbb = null;
    Range zRange = null;
    CalendarDateRange dateRange = null;
    List<String> gridList = new ArrayList<>();
    gridList.add("Temperature_surface");
    int stride_time = 1;
    int horizStride = 1;

    File outFile = tempFolder.newFile();

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outFile.getAbsolutePath());
    CFGridWriter2.writeFile(gds, gridList,
            null, null, horizStride,
            zRange,
            dateRange, stride_time,
            addLatLon,
            writer);

    assert outFile.exists();
    try ( GridDataset result = ucar.nc2.dt.grid.GridDataset.open(outFile.getAbsolutePath())) {
      System.out.printf("result = %s%n", result.getLocation());
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
