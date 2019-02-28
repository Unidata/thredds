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
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@RunWith(JUnit4.class)
public class TestMRMS {
  static final String testfile = "../grib/src/test/data/MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2.gz";

  @Test
  public void checkVariable() throws IOException {
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {
      Variable var = nc.findVariable("LowLevelCompositeReflectivity_altitude_above_msl");
      Assert.assertNotNull(var);

      Attribute att = var.findAttribute("missing_value");
      Assert.assertNotNull(att);
      Assert.assertEquals(-99., att.getNumericValue().doubleValue(), 1e-6);

      att = var.findAttribute("_FillValue");
      Assert.assertNotNull(att);
      Assert.assertEquals(-999., att.getNumericValue().doubleValue(), 1e-6);
    }
  }
}
