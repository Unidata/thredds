/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.grib2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

@RunWith(JUnit4.class)
public class TestDataTemplate {
    // Tests reading data using template 5.0
    @Test
    public void testDrs0() throws IOException {
        final String testfile = "../grib/src/test/data/Eumetsat.VerticalPerspective.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("Pixel_scene_type");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertTrue(Float.isNaN(data[0]));
            Assert.assertEquals(101.0, data[584*1237+632], 1e-6);
        }
    }

    // Tests reading data using template 5.2
    @Test
    public void testDrs2() throws IOException {
        final String testfile = "../grib/src/test/data/ds.snow.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("Total_snowfall_surface_6_Hour_Accumulation");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertTrue(Float.isNaN(data[0]));
            Assert.assertTrue(Float.isNaN(data[1234]));
        }
    }

    // Tests reading data using template 5.3
    @Test
    public void testDrs3() throws IOException {
        final String testfile = "../grib/src/test/data/ds.sky.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("Total_cloud_cover_surface");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertTrue(Float.isNaN(data[0]));
            Assert.assertEquals(23.0, data[37*177 + 114], 1e-6);
        }
    }

    // Tests reading data using template 5.40
    @Test
    public void testDrs40() throws IOException {
        final String testfile = "../grib/src/test/data/pdsScale.pds1.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("Temperature_isobaric_ens");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertEquals(263.57705688, data[0], 1e-6);
            Assert.assertEquals(263.70205688, data[1234], 1e-6);
        }
    }

    // Tests reading data using template 5.41
    @Test
    public void testPng() throws IOException {
        final String testfile = "../grib/src/test/data/MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("LowLevelCompositeReflectivity_altitude_above_msl");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertEquals(-99., data[15], 1e-6);
            Assert.assertEquals(18.5, data[5602228], 1e-6);
        }
    }

    // Tests reading data using template 5.41 with a bitmap
    @Test
    public void testPngBitmap() throws IOException {
        final String testfile = "../grib/src/test/data/HLYA10.grib2";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("VAR0-19-223_FROM_7-212--1_isobaric");
            float[] data = (float[]) var.read().get1DJavaArray(DataType.FLOAT);

            Assert.assertEquals(0.36976, data[13], 1e-5);
            Assert.assertTrue(Double.isNaN(data[15]));
        }
    }
}
