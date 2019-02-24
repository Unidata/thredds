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
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@RunWith(JUnit4.class)
public class TestDataTemplate {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
