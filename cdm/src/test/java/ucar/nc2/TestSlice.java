/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import java.io.IOException;
import static org.junit.Assert.assertEquals;

public class TestSlice {
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
    filePath = tempFolder.newFile("testSlice.nc").getAbsolutePath();

    try (NetcdfFileWriteable file = NetcdfFileWriteable.createNew(filePath)) {
      Dimension t   = new Dimension("t", DIM_T, true);
      Dimension alt = new Dimension("alt", DIM_ALT, true);
      Dimension lat = new Dimension("lat", DIM_LAT, true);
      Dimension lon = new Dimension("lon", DIM_LON, true);
      file.addDimension(null, t);
      file.addDimension(null, alt);
      file.addDimension(null, lat);
      file.addDimension(null, lon);

      Dimension[] dims = { t, alt, lat, lon };
      file.addVariable(DATA_VARIABLE, DataType.FLOAT, dims);
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
    try (NetcdfFileWriteable file = NetcdfFileWriteable.openExisting(filePath)) {
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
