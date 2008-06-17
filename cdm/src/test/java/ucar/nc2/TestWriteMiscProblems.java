/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.ma2.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 16, 2008
 */
public class TestWriteMiscProblems extends TestCase {
  private boolean show = false;

  public TestWriteMiscProblems(String name) {
    super(name);
  }

  public void testWriteBigString() throws IOException {
    String filename = TestLocal.cdmTestDataDir + "testWriteMisc.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, false);

    int len = 120000;
    ArrayChar.D1 arrayCharD1 = new ArrayChar.D1(len);
    for (int i = 0; i < len; i++)
      arrayCharD1.set(i, '1');
    ncfile.addGlobalAttribute("tooLongChar", arrayCharD1);

    char[] carray = new char[len];
    for (int i = 0; i < len; i++)
      carray[i] = '2';
    String val = new String(carray);
    ncfile.addGlobalAttribute("tooLongString", val);


    ncfile.create();
    ncfile.close();
  }

  public void testBig() throws IOException, InvalidRangeException {

    long start = System.nanoTime();
    System.out.println("Begin <=");

    String varName = "example";

    int timeSize = 8;
    int latSize = 8022;
    int lonSize = 10627;

//	int timeSize = (int) Math.pow(2, 3);
//	int latSize = (int) Math.pow(2, 13);
//	int lonSize = (int) Math.pow(2, 13);

    System.out.println("File size  (B)  = " + (long) timeSize * latSize * lonSize * 4);
    System.out.println("File size~ (MB) = " + Math.round((long) timeSize * latSize * lonSize * 4 / Math.pow(2, 20)));

    NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew("C:/temp/bigFile2.nc");

    ncFile.setFill(false);
    ncFile.setLargeFile(true);

    String timeUnits = "hours since 2008-06-06 12:00:0.0";
    String coordUnits = "degrees";

    Dimension[] dim = new Dimension[3];

    dim[0] = setDimension(ncFile, "time", timeUnits, timeSize);
    dim[1] = setDimension(ncFile, "lat", coordUnits, latSize);
    dim[2] = setDimension(ncFile, "lon", coordUnits, lonSize);

    ncFile.addVariable(varName, DataType.FLOAT, dim);

    ncFile.addVariableAttribute(varName, "_FillValue", -9999);
    ncFile.addVariableAttribute(varName, "missing_value", -9999);

    System.out.println("Creating netcdf <=");
    ncFile.create();
    long stop = System.nanoTime();
    double took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took "+took+" secs");
    start = stop;

//	Array ar;
//
//	float[] baseArray = new float[latSize];
//	for (int i = 0; i < latSize; i++) {
//	    baseArray[i] = (float) (i);
//	}
//	ar = Array.factory(baseArray);
//	ncFile.write("lat", Array.factory(baseArray));
//	baseArray = new float[lonSize];
//	for (int i = 0; i < lonSize; i++) {
//	    baseArray[i] = (float) (i);
//	}
//	ar = Array.factory(baseArray);
//	ncFile.write("lon", Array.factory(baseArray));
//
//	baseArray = new float[timeSize];
//	for (int i = 0; i < timeSize; i++) {
//	    baseArray[i] = (float) (i * 3);
//	}
//	ar = Array.factory(baseArray);
//	ncFile.write("time", ar);

    System.out.println("Writing netcdf <=");

    int[] shape = new int[]{timeSize, 1, lonSize};
    float[] floatStorage = new float[timeSize * lonSize];
    Array floatArray = Array.factory(float.class, shape, floatStorage);
    for (int i = 0; i < latSize; i++) {
      int[] origin = new int[]{0, i, 0};
      ncFile.write(varName, origin, floatArray);
    }

//	int[] shape = new int[] { timeSize, latSize, 1};
//	float[] floatStorage = new float[timeSize * latSize];
//	Array floatArray = Array.factory(float.class, shape, floatStorage);
//	for (int i = 0; i < lonSize; i++) {
//	    int[] origin = new int[] { 0, 0, i };
//	    ncFile.write(varName, origin, floatArray);
//	}

    ncFile.close();

    System.out.println("Done <=");
    stop = System.nanoTime();
    took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took "+took+" secs");
    start = stop;
  }

  private static Dimension setDimension(NetcdfFileWriteable ncFile, String name, String units, int length) {

    Dimension dimension = ncFile.addDimension(name, length);
    ncFile.addVariable(name, DataType.FLOAT, new Dimension[]{dimension});
    ncFile.addVariableAttribute(name, "units", units);

    return dimension;
  }


}
