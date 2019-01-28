package ucar.nc2.ft2.simpgeometry;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

/**
 * Test class for Simple Geometry Helper
 * 
 * @author wchen@usgs.gov
 **/
public class CFTestSimpleGeometryHelper {

	public final String file = "huc_helper_test.nc";
	public final String fileReverse = "hru_soil_moist_3hru_5timestep.nc";
	String path = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/" + file;
	String pathReverse = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/" + fileReverse;
	NetcdfDataset dataset;
	NetcdfDataset datasetReverse;
	
	
	@Test
	public void testIDFirstTimeSecondReadStrAll() {
		Variable var = dataset.findVariable("et");
		String str = CFSimpleGeometryHelper.getSubsetString(var, 0);
		Assert.assertEquals("0,:", str);
	}
	
	@Test
	public void testTimeFirstIDSecondReadStrAll() throws IOException {
		Variable var = datasetReverse.findVariable("hru_soil_moist");
		String str = CFSimpleGeometryHelper.getSubsetString(var, 3);
		Assert.assertEquals(":,3", str);
	}
	
	@Test
	public void testIDFirstTimeSecondReadStrSub() {
		Variable var = dataset.findVariable("et");
		String str = CFSimpleGeometryHelper.getSubsetString(var, 1, 2, 0);
		Assert.assertEquals("0,1:2", str);
	}
	
	@Test
	public void testTimeFirstIDSecondReadStrSub() throws IOException {
		Variable var = datasetReverse.findVariable("hru_soil_moist");
		String str = CFSimpleGeometryHelper.getSubsetString(var, 0, 3, 2);
		Assert.assertEquals("0:3,2", str);
	}
	
	@Test
	public void testOneDimArrayStr() {
		Variable var = dataset.findVariable("SHAPE_Leng");
		String str = CFSimpleGeometryHelper.getSubsetString(var, 0);
		Assert.assertEquals("0",str);
	}
	
	
	public CFTestSimpleGeometryHelper() throws IOException{
		dataset = NetcdfDataset.openDataset(path);
		datasetReverse = NetcdfDataset.openDataset(pathReverse);
	}
}
