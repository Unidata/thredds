package ucar.nc2.ft2.simpgeometry;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.simpgeometry.GeometryType;
import ucar.nc2.ft2.simpgeometry.Line;
import ucar.nc2.ft2.simpgeometry.Point;
import ucar.nc2.ft2.simpgeometry.Polygon;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryReader;
import ucar.unidata.util.test.TestDir;

/**
 * Various tests using Simple Geometry Reader and its ability to read and pass back Geometries correctly.
 * 
 * @author wchen@usgs.gov
 *
 */
public class TestSimpleGeometryReader {
	
	private SimpleGeometryReader openReaderOverFile(String filename) throws IOException {
		String filepath = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/" + filename;
		NetcdfDataset dataset = null;
		dataset = NetcdfDataset.openDataset(filepath);
		return new SimpleGeometryReader(dataset);
	}
	
	@Test
	public void testReaderPoint() throws IOException {
		SimpleGeometryReader rdr = openReaderOverFile("avg_temp_3gage_5timesteps.nc");
		Assert.assertNotNull(rdr);
		Point point0 = rdr.readPoint("gage_avg_temp", 0);
		Point point1 = rdr.readPoint("gage_avg_temp", 1);
		Point point2 = rdr.readPoint("gage_avg_temp", 2);
		
		// Test point data
		Assert.assertEquals(-91.277, point0.getX(), 0.0001); Assert.assertEquals(40.75365, point0.getY(), 0.000001);
		Assert.assertEquals(-91.674, point1.getX(), 0.0003); Assert.assertEquals(40.9253, point1.getY(), 0.00001);
		Assert.assertEquals(-91.5515, point2.getX(), 0.00006); Assert.assertEquals(41, point2.getY(), 0.1);
		
		//Test data
		Assert.assertEquals(0.623, point0.getData().getDouble(0), 0.001);
		Assert.assertEquals(9.02, point1.getData().getDouble(3), 0.001);
		Assert.assertEquals(3.14, point2.getData().getDouble(1), 0.001);
	}
	
	@Test
	public void testReaderLine() throws IOException {
		SimpleGeometryReader rdr = openReaderOverFile("outflow_3seg_5timesteps.nc");
		Assert.assertNotNull(rdr);
		
		Line line0 = rdr.readLine("seg_outflow", 0);
		Line line1 = rdr.readLine("seg_outflow", 1);
		Line line2 = rdr.readLine("seg_outflow", 2);
		
		// Test data
		Assert.assertEquals(0.462, line0.getData().getDouble(0), 0.001);
		Assert.assertEquals(3.61, line1.getData().getDouble(3), 0.01);
		Assert.assertEquals(9.49, line2.getData().getDouble(1), 0.01);
		
		// Test point amounts
		Assert.assertEquals(1125, line0.getPoints().size());
		Assert.assertEquals(10, line1.getPoints().size());
		Assert.assertEquals(2280, line2.getPoints().size());
	}
	
	@Test
	public void testReaderPolygonsMultiPolygons() throws IOException {

		double err = 0.01;
			
		SimpleGeometryReader rdr = openReaderOverFile("hru_soil_moist_3hru_5timestep.nc");
		Polygon poly = rdr.readPolygon("hru_soil_moist", 0);
		Polygon poly2 = rdr.readPolygon("hru_soil_moist", 1);
		Polygon poly3 = rdr.readPolygon("hru_soil_moist", 2);
		
		Assert.assertEquals(6233, poly.getPoints().size());
		Assert.assertEquals(5, poly2.getPoints().size());
		Assert.assertEquals(6033, poly2.getNext().getPoints().size());
		Assert.assertEquals(5135, poly3.getPoints().size());
		
		// Test data retrieval
		Assert.assertEquals(1.28, poly.getData().getDouble(2), err);
		Assert.assertEquals(1.52, poly2.getData().getDouble(3), err);
		Assert.assertEquals(1.36, poly3.getData().getDouble(0), err);
	}
	
	@Test
	public void testGetPointGeometryType() throws IOException {
		SimpleGeometryReader rdr = openReaderOverFile("avg_temp_3gage_5timesteps.nc");
		Assert.assertEquals(GeometryType.POINT, rdr.getGeometryType("gage_avg_temp"));
	}
	
	@Test
	public void testGetLineGeometryType() throws IOException {
		SimpleGeometryReader rdr = openReaderOverFile("outflow_3seg_5timesteps.nc");
		Assert.assertEquals(GeometryType.LINE, rdr.getGeometryType("seg_outflow"));
	}
	
	@Test
	public void testGetPolygonGeometryType() throws IOException {
		SimpleGeometryReader rdr = openReaderOverFile("hru_soil_moist_3hru_5timestep.nc");
		Assert.assertEquals(GeometryType.POLYGON, rdr.getGeometryType("hru_soil_moist"));
	}
}
