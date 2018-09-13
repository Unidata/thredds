package ucar.nc2.dataset.conv;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.unidata.util.test.TestDir;

public class TestWRFTime {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

	@Test
	public void testWrfNoTimeVar() throws IOException {
		String tstFile = TestDir.cdmLocalTestDataDir +"wrf/WrfNoTimeVar.nc";
		logger.info("Open '{}'", tstFile);
		Set<NetcdfDataset.Enhance> defaultEnhanceMode = NetcdfDataset.getDefaultEnhanceMode();
		EnumSet<NetcdfDataset.Enhance> enhanceMode = EnumSet.copyOf(defaultEnhanceMode);
		enhanceMode.add(NetcdfDataset.Enhance.IncompleteCoordSystems);
		DatasetUrl durl = DatasetUrl.findDatasetUrl (tstFile);
		NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, enhanceMode, null);

		List<CoordinateSystem> cs = ncd.getCoordinateSystems();
		Assert.assertEquals(1, cs.size());
		CoordinateSystem dsCs = cs.get(0);
		Assert.assertEquals(2, dsCs.getCoordinateAxes().size());

		VariableDS var = (VariableDS) ncd.findVariable("T2");
		List<CoordinateSystem> varCs = var.getCoordinateSystems();
		Assert.assertEquals(1, varCs.size());
		Assert.assertEquals(dsCs, varCs.get(0));
	}
}
