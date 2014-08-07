package ucar.nc2.dt.grid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Range;
import ucar.nc2.TestLocal;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.test.util.TestDir;

/**
 * Created by lesserwhirls on 7/28/14.
 */
public class TestNetcdfCFWriter {

    GridDataset gds = null;

    @Before
    public void readDataset() {
        String fileIn = TestDir.cdmLocalTestDataDir + "testNetcdfCFWriter.nc4";
        try {
            gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);
        } catch (Exception exc) {
            exc.printStackTrace();
        };
    }

    @Test
    public void testNullLatLonBB() {
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
            NetcdfCFWriter writer = new NetcdfCFWriter();
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
