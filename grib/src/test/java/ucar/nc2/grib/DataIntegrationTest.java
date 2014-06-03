package ucar.nc2.grib;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

public class DataIntegrationTest {

	public static double PRECISION = 0.0;

	@Test
	public void testGrib1Regular() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("temperature_regular.grib", "2_metre_temperature_surface");
		assertEquals(1038240, data.length);
		assertEquals(247.56875610351562, data[0], PRECISION);
		assertEquals(301.4125061035156, data[data.length / 2], PRECISION);
		assertEquals(228.47500610351562, data[data.length - 1], PRECISION);	
	}

	@Test
	public void testGrib2Regular() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("temperature_regular.grib2", "Temperature_hybrid");
		assertEquals(4150080, data.length);
		assertEquals(247.0420684814453, data[0], PRECISION);
		assertEquals(282.60455322265625, data[data.length / 2], PRECISION);
		assertEquals(229.2608184814453, data[data.length - 1], PRECISION);	
	}

	@Test
	public void testGrib1RegularBitmap() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("seatemperature_regular_bitmap.grib", "Sea_surface_temperature_surface");
		assertEquals(1038240, data.length);
		assertEquals(271.459716796875, data[0], PRECISION);
		assertEquals(302.472412109375, data[data.length / 2], PRECISION);
		assertEquals(Double.NaN, data[data.length - 1], PRECISION);	
	}
	
	@Test
	public void testGrib1Reduced() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("temperature_reduced.grib", "2_metre_temperature_surface");
		assertEquals(3276800, data.length);
		assertEquals(262.1839599609375, data[0], PRECISION);
		assertEquals(299.9652099609375, data[data.length / 2], PRECISION);
		assertEquals(231.87254333496094, data[data.length - 1], PRECISION);	
	}

	@Test
	public void testGrib2Reduced() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("temperature_reduced.grib2", "Temperature_hybrid");
		assertEquals(1280000, data.length);
		assertEquals(209.39283752441406, data[0], PRECISION);
		assertEquals(226.64283752441406, data[data.length / 2], PRECISION);
		assertEquals(226.4555206298828, data[data.length - 1], PRECISION);	
	}

	@Test
	public void testGrib1ReducedBitmap() throws IOException, InvalidRangeException, URISyntaxException {
		double[] data = fetchVariable("seatemperature_reduced_bitmap.grib", "Sea_surface_temperature_surface");
		assertEquals(1280000, data.length);
		assertEquals(271.4599609375, data[0], PRECISION);
		assertEquals(302.3330078125, data[data.length / 2], PRECISION);
		assertEquals(Double.NaN, data[data.length - 1], PRECISION);	
	}

	private double[] fetchVariable(String filename, String variableShortName) throws URISyntaxException, IOException, InvalidRangeException {
		URL dir_url = ClassLoader.getSystemResource(filename);
		File file = new File(dir_url.toURI());
		GridDataset gds = GridDataset.open(file.getAbsolutePath());
		GridDatatype gdt = gds.findGridByShortName(variableShortName);
		return (double[]) gdt.readDataSlice(0, 0, -1, -1).get1DJavaArray(double.class);
	}
	
}
