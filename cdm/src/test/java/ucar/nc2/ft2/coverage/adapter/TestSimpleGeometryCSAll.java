package ucar.nc2.ft2.coverage.adapter;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.simpgeometry.GeometryType;
import ucar.nc2.ft2.simpgeometry.Polygon;
import ucar.nc2.ft2.simpgeometry.SimpleGeometry;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeature;
import ucar.nc2.ft2.simpgeometry.adapter.SimpleGeometryCSBuilder;
import ucar.unidata.util.test.TestDir;

/**
 * Tests setting up a simple geometry feature type using SimpleGeometryCS
 * and SimpleGeometryCSBuilder as well as SimpleGeometryCoverage.
 * 
 * @author wchen
 *
 */
public class TestSimpleGeometryCSAll {
	@Test
	public void testSimpleGeometryBuildCS() throws IOException, InvalidRangeException
	{
		NetcdfDataset data = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_3hru_5timestep.nc");
		List<CoordinateSystem> csl = data.getCoordinateSystems();
		SimpleGeometryCSBuilder builder = new SimpleGeometryCSBuilder(data, csl.get(0), null);
		Variable hru_test = data.findVariable("hru_soil_moist");
		
		SimpleGeometryFeature sgc = new SimpleGeometryFeature(hru_test.getFullNameEscaped(), hru_test.getDataType(), hru_test.getAttributes(), csl.get(0).getName(), hru_test.getUnitsString(),
				hru_test.getDescription(), null, GeometryType.POLYGON);
		sgc.setCoordSys(builder.makeCoordSys());
		
		// Test retrieval of axis
		Assert.assertEquals("catchments_x", sgc.getXAxis().getFullNameEscaped());
		Assert.assertEquals("catchments_y", sgc.getYAxis().getFullNameEscaped());
		Assert.assertEquals("hruid", sgc.getIDAxis().getFullNameEscaped());
		Assert.assertEquals(null, sgc.getZAxis());
		
		// Now test reading a geometry.
		SimpleGeometry testGeometry = sgc.readGeometry(0);
		boolean testInstancePolygon = false;
		
		if(testGeometry instanceof Polygon) testInstancePolygon = true;
		Assert.assertEquals(true, testInstancePolygon);
		
		Polygon polygonTestGeometry = (Polygon) testGeometry;
		Assert.assertEquals(6233, polygonTestGeometry.getPoints().size());
	}
}
