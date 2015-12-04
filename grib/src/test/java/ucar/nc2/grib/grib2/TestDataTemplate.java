/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib.grib2;

import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

public class TestDataTemplate {
    // Tests reading data using template 5.41
    @Test
    public void testPng() throws IOException {
        final String testfile = "../grib/src/test/data/MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2.gz";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("LowLevelCompositeReflectivity_altitude_above_msl");
            float[] data = (float[]) var.read().get1DJavaArray(float.class);

            Assert.assertEquals(-99., data[15], 1e-6);
            Assert.assertEquals(18.5, data[5602228], 1e-6);
        }
    }

    // Tests reading data using template 5.41 with a bitmap
    @Test
    public void testPngBitmap() throws IOException {
        final String testfile = "../grib/src/test/data/HLYA10";
        try (NetcdfFile nc = NetcdfFile.open(testfile)) {
            Variable var = nc.findVariable("VAR0-19-223_FROM_7-212--1_isobaric");
            float[] data = (float[]) var.read().get1DJavaArray(float.class);

            Assert.assertEquals(0.36976, data[13], 1e-5);
            Assert.assertTrue(Double.isNaN(data[15]));
        }
    }
}
