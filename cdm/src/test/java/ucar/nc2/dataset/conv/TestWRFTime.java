package ucar.nc2.dataset.conv;

import java.io.IOException;

import org.junit.Test;

import ucar.ma2.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

public class TestWRFTime {

    @Test
    public void testWrfTimeUnderscore() throws IOException {
    	String tstFile = TestDir.cdmLocalTestDataDir +"wrf/WrfTimesStrUnderscore.nc";
    	System.out.println(tstFile);
    	String wrfConvention = new WRFConvention().getConventionUsed();
    	NetcdfDataset ncd = NetcdfDataset.openDataset(tstFile);
    	// make sure this file went through the WrfConvention
    	assert ncd.getConventionUsed().equals(wrfConvention);
    	CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
    	Array times = tca.read();
    	ncd.close();
    	// if the date/time string cannot be parsed (in this case, 2008-06-27_00:00:00)
    	// the the time will come back as 0 seconds after going through the WRFConvention
    	// coordinate system builder class.
    	assert times.getInt(0) != 0;
    	// first date in this file is 1214524800 [seconds since 1970-01-01T00:00:00],
    	// which is 2008-06-27 00:00:00
    	assert times.getInt(0) == 1214524800;
    }
}
