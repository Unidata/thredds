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

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.IO;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 16, 2008
 */
public class TestWriteMiscProblems extends TestCase {

  public TestWriteMiscProblems(String name) {
    super(name);
  }

  public void testWriteBigString() throws IOException {
    String filename = TestLocal.temporaryDataDir + "testWriteMisc.nc";
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

    System.out.println("File size  (B)  = " + (long) timeSize * latSize * lonSize * 4);
    System.out.println("File size~ (MB) = " + Math.round((long) timeSize * latSize * lonSize * 4 / Math.pow(2, 20)));

    NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(TestAll.temporaryLocalDataDir + "bigFile2.nc");
    ncFile.setFill(false);
    ncFile.setLargeFile(true);

    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    ncFile.setLength(approxSize);

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
    System.out.println("That took " + took + " secs");
    start = stop;

    System.out.println("Writing netcdf <=");

    int[] shape = new int[]{1, 1, lonSize};
    float[] floatStorage = new float[lonSize];
    Array floatArray = Array.factory(float.class, shape, floatStorage);
    for (int t = 0; t < timeSize; t++) {
      for (int i = 0; i < latSize; i++) {
        int[] origin = new int[]{t, i, 0};
        ncFile.write(varName, origin, floatArray);
      }
    }

    ncFile.close();

    System.out.println("Done <=");
    stop = System.nanoTime();
    took = (stop - start) * .001 * .001 * .001;
    System.out.println("That took " + took + " secs");
    start = stop;
  }

  private static Dimension setDimension(NetcdfFileWriteable ncFile, String name, String units, int length) {

    Dimension dimension = ncFile.addDimension(name, length);
    ncFile.addVariable(name, DataType.FLOAT, new Dimension[]{dimension});
    ncFile.addVariableAttribute(name, "units", units);

    return dimension;
  }


  public void testCharMultidim() throws IOException, InvalidRangeException {

    /* dimension lengths */
    final int Time_len = 0;
    final int DateStrLen_len = 19;

    /* enter define mode */
    String filename = TestLocal.temporaryDataDir + "testCharMultidim.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, true);

    /* define dimensions */
    Dimension Time_dim = ncfile.addUnlimitedDimension("Time");
    Dimension DateStrLen_dim = ncfile.addDimension("DateStrLen", DateStrLen_len);

    /* define variables */
    List<Dimension> Times_dimlist = new ArrayList<Dimension>();
    Times_dimlist.add(Time_dim);
    Times_dimlist.add(DateStrLen_dim);
    ncfile.addVariable("Times", DataType.CHAR, Times_dimlist);
    ncfile.create();

    /* assign variable data */
    String contents = "2005-04-11_12:00:002005-04-11_13:00:00";
    ArrayChar data = new ArrayChar(new int[]{2, 19});
    IndexIterator iter = data.getIndexIterator();
    int count = 0;
    while (iter.hasNext())
      iter.setCharNext(contents.charAt(count++));
    ncfile.write("Times", data);
    ncfile.close();

    NetcdfFile nc = NetcdfFile.open(filename, null);
    Variable v = nc.findTopVariable("Times");
    Array dataRead = v.read();
    assert dataRead instanceof ArrayChar;
    ArrayChar dataC = (ArrayChar) dataRead;
    for (int i=0; i<2; i++)
      System.out.printf("%d == %s %n", i, dataC.getString(i));
    assert dataC.getString(0).equals("2005-04-11_12:00:00");
    assert dataC.getString(1).equals("2005-04-11_13:00:00");

    nc.close();
  }

  public void testRemove() throws IOException, InvalidRangeException {
    try {
      String inName = TestLocal.cdmTestDataDir + "testWrite.nc";
      String outName = TestLocal.temporaryDataDir + "testRemove.nc";

      NetcdfDataset ncd = NetcdfDataset.acquireDataset(inName, null);
      System.out.println(ncd);
      System.out.println(ncd.removeVariable(null, "temperature"));
      System.out.println(ncd);

      ncd.finish();

      NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, outName, true);
      ncdnew.close();
      ncd.close();

      NetcdfDataset ncdnew2 = NetcdfDataset.acquireDataset(outName, null);
      assert ncdnew2.findVariable("temperature") == null;

    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  public void testRedefine() throws IOException, InvalidRangeException {
    String org = TestLocal.cdmTestDataDir + "testWriteRecord.nc";
    String path = TestLocal.temporaryDataDir + "testWriteRecordRedefine.nc";
    File orgFile = new File(org);
    File newFile = new File(path);
    if (newFile.exists()) newFile.delete();
    IO.copyFile(orgFile, newFile);

    NetcdfFileWriteable ncfile = null;
    try {
      ncfile = NetcdfFileWriteable.openExisting(path, false);
      System.out.println(ncfile);
      
      ncfile.setRedefineMode(true);

      Variable tvar = ncfile.findVariable("T");
      ncfile.setExtraHeaderBytes(1024);
      ncfile.addVariable("header_data", DataType.FLOAT, tvar.getDimensions());
      System.out.println(ncfile);

    } finally {
      try {
        if (ncfile != null) {
          ncfile.setRedefineMode(false);
          ncfile.flush();
          ncfile.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  public void utestWarning() throws IOException {
    String org = "C:/tmp/example.nc";
    NetcdfDataset dds = NetcdfDataset.openDataset(org);
    FileWriter.writeToFile(dds, "C:/tmp/testOut.nc", true);
    dds.close();
  }

  
}
