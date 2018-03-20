/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;

public class TestSlice {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DATA_VARIABLE = "data";
  private static final int DIM_T = 10;
  private static final int DIM_ALT = 5;
  private static final int DIM_LAT = 123;
  private static final int DIM_LON = 234;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String filePath;

  @Before
  public void setUp() throws IOException {
    filePath = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filePath)) {
      file.addDimension(null, "t", DIM_T);
      file.addDimension(null, "alt", DIM_ALT);
      file.addDimension(null, "lat", DIM_LAT);
      file.addDimension(null, "lon", DIM_LON);

      file.addVariable(DATA_VARIABLE, DataType.FLOAT, "t alt lat lon");
      file.create();
    }
  }

  private Array createData() {
    ArrayFloat.D4 values = new ArrayFloat.D4(DIM_T, DIM_ALT, DIM_LAT, DIM_LON);
    for (int i = 0; i < DIM_T; i++) {
      for (int j = 0; j < DIM_ALT; j++) {
        for (int k = 0; k < DIM_LAT; k++) {
          for (int l = 0; l < DIM_LON; l++) {
            values.set(i, j, k, l, i + j);
          }
        }
      }
    }
    return values;
  }

  @Test
  public void testFill() throws IOException, InvalidRangeException {
    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filePath)) {
      file.write(DATA_VARIABLE, createData());
    }
  }

  @Test
  public void testSlice1() throws IOException, InvalidRangeException {
    try (NetcdfFile file = NetcdfFile.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced = var.slice(0, 3);
      sliced.read();

      int[] shape = sliced.getShape();
      assertEquals(3, shape.length);
      assertEquals(DIM_ALT, shape[0]);
      assertEquals(DIM_LAT, shape[1]);
      assertEquals(DIM_LON, shape[2]);

      assertEquals("alt lat lon", sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice2() throws IOException, InvalidRangeException {
    try (NetcdfFile file = NetcdfFile.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced = var.slice(1, 3);
      sliced.read();

      int[] shape = sliced.getShape();
      assertEquals(3, shape.length);
      assertEquals(DIM_T, shape[0]);
      assertEquals(DIM_LAT, shape[1]);
      assertEquals(DIM_LON, shape[2]);

      assertEquals("t lat lon", sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice3() throws IOException, InvalidRangeException {
    try (NetcdfFile file = NetcdfFile.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced1 = var.slice(0, 3);
      Variable sliced2 = sliced1.slice(0, 3);

      int[] shape = sliced2.getShape();
      assertEquals(2, shape.length);
      assertEquals(DIM_LAT, shape[0]);
      assertEquals(DIM_LON, shape[1]);

      assertEquals("lat lon", sliced2.getDimensionsString());

      Array org = var.read("3,3,:,:");
      Array data = sliced2.read();
      ucar.unidata.util.test.CompareNetcdf.compareData(org, data);
    }
  }
}
