/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import org.junit.Assert;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.lang.invoke.MethodHandles;

/**
 * Test reading GRIB2 files with {@link RotatedPole} projections.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
@RunWith(JUnit4.class)
public class TestRotatedPole {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Tolerance for floating point comparisons.
   */
  public static final double DELTA = 1e-6;

  /**
   * Test reading an RAP native GRIB2 file with a GDS template 32769
   * {@link RotatedPole} projection.
   */
  @Test
  public void testRapNative() throws Exception {
    try (NetcdfFile nc = NetcdfFile.open("../grib/src/test/data/rap-native.grib2")) {
      Assert.assertNotNull(nc);
      // check dimensions
      Dimension rlonDim = nc.findDimension("rlon");
      Assert.assertNotNull(rlonDim);
      Assert.assertEquals(7, rlonDim.getLength());
      Dimension rlatDim = nc.findDimension("rlat");
      Assert.assertNotNull(rlatDim);
      Assert.assertEquals(5, rlatDim.getLength());
      Dimension timeDim = nc.findDimension("time");
      Assert.assertNotNull(timeDim);
      Assert.assertEquals(1, timeDim.getLength());
      // check coordinate variables
      Variable rlonVar = nc.findVariable("rlon");
      Assert.assertNotNull(rlonVar);
      Assert.assertEquals(1, rlonVar.getDimensions().size());
      Assert.assertEquals(rlonDim, rlonVar.getDimensions().get(0));
      Assert.assertEquals("grid_longitude", rlonVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("degrees", rlonVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new float[] { -30, -20, -10, 0, 10, 20, 30 },
          (float[]) rlonVar.read().copyTo1DJavaArray(), (float) DELTA);
      Variable rlatVar = nc.findVariable("rlat");
      Assert.assertNotNull(rlatVar);
      Assert.assertEquals(1, rlatVar.getDimensions().size());
      Assert.assertEquals(rlatDim, rlatVar.getDimensions().get(0));
      Assert.assertEquals("grid_latitude", rlatVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("degrees", rlatVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new float[] { -20, -10, 0, 10, 20 }, (float[]) rlatVar.read().copyTo1DJavaArray(),
          (float) DELTA);
      Variable timeVar = nc.findVariable("time");
      Assert.assertNotNull(timeVar);
      Assert.assertEquals(1, timeVar.getDimensions().size());
      Assert.assertEquals(timeDim, timeVar.getDimensions().get(0));
      Assert.assertEquals("time", timeVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("Hour since 2016-04-25T22:00:00Z", timeVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new double[] { 0 }, (double[]) timeVar.read().copyTo1DJavaArray(), DELTA);
      // check projection variable
      Variable projVar = nc.findVariable("RotatedLatLon32769_Projection");
      Assert.assertNotNull(projVar);
      Assert.assertEquals("rotated_latitude_longitude", projVar.findAttribute("grid_mapping_name").getStringValue());
      Assert.assertEquals(74.0, projVar.findAttribute("grid_north_pole_longitude").getNumericValue().doubleValue(),
          DELTA);
      Assert.assertEquals(36.0, projVar.findAttribute("grid_north_pole_latitude").getNumericValue().doubleValue(),
          DELTA);
      // check data variable
      Variable dataVar = nc.findVariable("Temperature_surface");
      Assert.assertNotNull(dataVar);
      Assert.assertEquals("RotatedLatLon32769_Projection", dataVar.findAttribute("grid_mapping").getStringValue());
      Assert.assertEquals("K", dataVar.findAttribute("units").getStringValue());
      Assert.assertEquals(3, dataVar.getDimensions().size());
      Assert.assertEquals(timeDim, dataVar.getDimensions().get(0));
      Assert.assertEquals(rlatDim, dataVar.getDimensions().get(1));
      Assert.assertEquals(rlonDim, dataVar.getDimensions().get(2));
      Assert.assertArrayEquals(
          new float[] { 300, 299, 298, 297, 296, 295, 294, 299, 300, 299, 298, 297, 296, 295, 298, 299, 300, 299, 298,
              297, 296, 297, 298, 299, 300, 299, 298, 297, 296, 297, 298, 299, 300, 299, 298 },
          (float[]) dataVar.read().copyTo1DJavaArray(), (float) DELTA);
    }
  }

  /**
   * Test reading a COSMO EU GRIB2 file with a GDS template 1
   * {@link RotatedPole} projection.
   */
  @Test
  public void testCosmoEu() throws Exception {
    try (NetcdfFile nc = NetcdfFile.open("../grib/src/test/data/cosmo-eu.grib2")) {
      Assert.assertNotNull(nc);
      // check dimensions
      Dimension rlonDim = nc.findDimension("rlon");
      Assert.assertNotNull(rlonDim);
      Assert.assertEquals(5, rlonDim.getLength());
      Dimension rlatDim = nc.findDimension("rlat");
      Assert.assertNotNull(rlatDim);
      Assert.assertEquals(5, rlatDim.getLength());
      Dimension timeDim = nc.findDimension("time");
      Assert.assertNotNull(timeDim);
      Assert.assertEquals(1, timeDim.getLength());
      // check coordinate variables
      Variable rlonVar = nc.findVariable("rlon");
      Assert.assertNotNull(rlonVar);
      Assert.assertEquals(1, rlonVar.getDimensions().size());
      Assert.assertEquals(rlonDim, rlonVar.getDimensions().get(0));
      Assert.assertEquals("grid_longitude", rlonVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("degrees", rlonVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new float[] { -18, -8, 2, 12, 22 }, (float[]) rlonVar.read().copyTo1DJavaArray(),
          (float) DELTA);
      Variable rlatVar = nc.findVariable("rlat");
      Assert.assertNotNull(rlatVar);
      Assert.assertEquals(1, rlatVar.getDimensions().size());
      Assert.assertEquals(rlatDim, rlatVar.getDimensions().get(0));
      Assert.assertEquals("grid_latitude", rlatVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("degrees", rlatVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new float[] { -20, -10, 0, 10, 20 }, (float[]) rlatVar.read().copyTo1DJavaArray(),
          (float) DELTA);
      Variable timeVar = nc.findVariable("time");
      Assert.assertNotNull(timeVar);
      Assert.assertEquals(1, timeVar.getDimensions().size());
      Assert.assertEquals(timeDim, timeVar.getDimensions().get(0));
      Assert.assertEquals("time", timeVar.findAttribute("standard_name").getStringValue());
      Assert.assertEquals("Hour since 2010-03-29T00:00:00Z", timeVar.findAttribute("units").getStringValue());
      Assert.assertArrayEquals(new double[] { 0 }, (double[]) timeVar.read().copyTo1DJavaArray(), DELTA);
      // check projection variable
      Variable projVar = nc.findVariable("RotatedLatLon_Projection");
      Assert.assertNotNull(projVar);
      Assert.assertEquals("rotated_latitude_longitude", projVar.findAttribute("grid_mapping_name").getStringValue());
      Assert.assertEquals(-170.0, projVar.findAttribute("grid_north_pole_longitude").getNumericValue().doubleValue(),
          DELTA);
      Assert.assertEquals(40.0, projVar.findAttribute("grid_north_pole_latitude").getNumericValue().doubleValue(),
          DELTA);
      // check data variable
      Variable dataVar = nc.findVariable("Snow_depth_water_equivalent_surface");
      Assert.assertNotNull(dataVar);
      Assert.assertEquals("RotatedLatLon_Projection", dataVar.findAttribute("grid_mapping").getStringValue());
      Assert.assertEquals("kg.m-2", dataVar.findAttribute("units").getStringValue());
      Assert.assertEquals(3, dataVar.getDimensions().size());
      Assert.assertEquals(timeDim, dataVar.getDimensions().get(0));
      Assert.assertEquals(rlatDim, dataVar.getDimensions().get(1));
      Assert.assertEquals(rlonDim, dataVar.getDimensions().get(2));
      Assert.assertArrayEquals(new float[] { 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114,
          115, 116, 117, 118, 119, 120, 121, 122, 123, 124 }, (float[]) dataVar.read().copyTo1DJavaArray(),
          (float) DELTA);
    }
  }

}
