package ucar.nc2.grib;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.receiver.DataReceiver;

public class LinearDataIntegrationTest {

	public static double PRECISION = 0.0;
	
	public static int INTERPOLATION = QuasiRegular.INTERPOLATION_LINEAR;
	
	private static boolean DEBUG_OUTPUT_IMAGE = false;
	private static boolean DEBUG_OUTPUT_TEXT = false;

	@Test
	public void testGrib1Regular() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("temperature_regular.grib", "2_metre_temperature_surface", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "temperature_regular.grib.linear.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "temperature_regular.grib.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(1038240, length);
		assertEquals(247.56875610351562, rawData[0], PRECISION);
		assertEquals(301.4125061035156, rawData[length / 2], PRECISION);
		assertEquals(228.47500610351562, rawData[rawData.length - 1], PRECISION);
	}
	
	@Test
	public void testGrib2Regular() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("temperature_regular.grib2", "Temperature_hybrid", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "temperature_regular.grib2.linear.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "temperature_regular.grib2.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(4150080, length);
		assertEquals(247.0420684814453, rawData[0], PRECISION);
		assertEquals(282.60455322265625, rawData[length / 2], PRECISION);
		assertEquals(229.2608184814453, rawData[rawData.length - 1], PRECISION);
	}
	
	@Test
	public void testGrib1RegularBitmap() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("seatemperature_regular_bitmap.grib", "Sea_surface_temperature_surface", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "seatemperature_regular_bitmap.grib.linear.raw.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "seatemperature_regular_bitmap.grib.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(1038240, length);
		assertEquals(271.459716796875, rawData[0], PRECISION);
		assertEquals(302.472412109375, rawData[length / 2], PRECISION);
		assertEquals(Double.NaN, rawData[rawData.length - 1], PRECISION);
	}

	@Test
	public void testGrib1Reduced() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("temperature_reduced.grib", "2_metre_temperature_surface", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "temperature_reduced.grib.linear.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "temperature_reduced.grib.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(2140702, length);
		assertEquals(262.1839599609375, rawData[0], PRECISION);
		assertEquals(293.17578125, rawData[length / 2], PRECISION);
		assertEquals(231.87254333496094, rawData[rawData.length - 1], PRECISION);
	}
	
	@Test
	public void testGrib2Reduced() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("temperature_reduced.grib2", "Temperature_hybrid", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "temperature_reduced.grib2.linear.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "temperature_reduced.grib2.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(843490, length);
		assertEquals(209.39283752441406, rawData[0], PRECISION);
		assertEquals(226.53346252441406, rawData[length / 2], PRECISION);
		assertEquals(226.4555206298828, rawData[rawData.length - 1], PRECISION);
	}
	
	@Test
	public void testGrib1ReducedBitmap() throws IOException, InvalidRangeException, URISyntaxException {
		DataReceiver rawDataReceiver = fetchVariable("seatemperature_reduced_bitmap.grib", "Sea_surface_temperature_surface", INTERPOLATION);
		if (DEBUG_OUTPUT_IMAGE) Image.generateImage(rawDataReceiver, "seatemperature_reduced_bitmap.grib.linear.png", false);
		if (DEBUG_OUTPUT_TEXT) Text.generateText(rawDataReceiver, "seatemperature_reduced_bitmap.grib.linear.csv", false);
		double[] rawData = (double[]) rawDataReceiver.getArray().get1DJavaArray(double.class);
		int length = rawDataReceiver.getGds(0).getNpts();
		assertEquals(843490, length);
		assertEquals(271.4599609375, rawData[0], PRECISION);
		assertEquals(293.1767578125, rawData[length / 2], PRECISION);
		assertEquals(Double.NaN, rawData[rawData.length - 1], PRECISION);
	}

	private DataReceiver fetchVariable(String filename, String variableShortName, int interpolation) throws URISyntaxException, IOException, InvalidRangeException {
		URL dir_url = ClassLoader.getSystemResource(filename);
		File file = new File(dir_url.toURI());
		GridDataset gds = GridDataset.open(file.getAbsolutePath());
		GridDatatype gdt = gds.findGridByShortName(variableShortName);
		return ((GribIosp) gds.getNetcdfDataset().getIosp()).readData(
				gdt.getVariable().getOriginalVariable(), 
				((GeoGrid) gdt).generateSection(0, 0, 0, 0, -1, -1),
				interpolation);
	}
	
}
