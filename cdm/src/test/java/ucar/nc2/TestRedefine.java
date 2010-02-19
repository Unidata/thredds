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

import java.io.IOException;

import junit.framework.TestCase;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.util.CompareNetcdf;

public class TestRedefine extends TestCase {

  public TestRedefine( String name) {
    super(name);
  }

  String filename = TestLocal.temporaryDataDir + "testRedefine.nc";
  String filename2 = TestLocal.temporaryDataDir + "testRedefine2.nc";

  public void testRedefine() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.createNew(filename, true);

    file.addGlobalAttribute("Conventions", "globulate");
    file.addGlobalAttribute("history", "lava");
    file.addGlobalAttribute("att8", "12345678");

    Dimension time = file.addDimension("time", 4, true, false, false);
    Dimension dims[] = {time};

    /* Add time */
    file.addVariable("time", DataType.DOUBLE, dims);
    file.addVariableAttribute("time", "quantity", "time");
    file.addVariableAttribute("time", "units", "s");

    /* Add a dependent variable */
    file.addVariable("h", DataType.DOUBLE, dims);
    file.addVariableAttribute("h", "quantity", "Height");
    file.addVariableAttribute("h", "units", "m");
    try {
      file.create();
    } catch (IOException e) {
      e.printStackTrace(System.err);
      fail("IOException on creation");
    }

    double td[] = {1.0, 2.0, 3.0, 4.0};
    double hd[] = {0.0, 0.1, 0.3, 0.9};
    ArrayDouble.D1 ta = new ArrayDouble.D1(4);
    ArrayDouble.D1 ha = new ArrayDouble.D1(4);
    for (int i = 0; i < 4; i++) {
      ta.set(i, td[i]);
      ha.set(i, hd[i]);
    }

    file.write("time", ta);
    file.write("h", ha);

    //////////////////////////////////////////
    file.setRedefineMode(true);

    file.renameGlobalAttribute("history", "lamp");
    file.addGlobalAttribute("history", "final");
    file.deleteGlobalAttribute("Conventions");

    file.addVariableAttribute("h", "units", "meters"); // duplicates existing
    file.addVariableAttribute("h", "new", "stuff");
    file.renameVariableAttribute("time", "quantity", "quality");

    file.renameVariable("time", "date");
    file.renameDimension("time", "date");

    /////////////////////////////////////////////////
    file.setRedefineMode(false);

    Attribute att = file.findGlobalAttribute("Conventions");
    assert att == null;
    att = file.findGlobalAttribute("history");
    assert att.getStringValue().equals("final");
    att = file.findGlobalAttribute("lamp");
    assert att.getStringValue().equals("lava");
    
    Variable v = file.findVariable("h");
    att = v.findAttribute("units");
    assert att != null;
    assert att.getStringValue().equals("meters");

    assert file.findVariable("time") == null;
    v = file.findVariable("date");
    assert v != null;
    assert v.getRank() == 1;
    assert null != v.findAttribute("quality");
    
    Dimension d = v.getDimension(0);
    assert d.getName().equals("date");

    assert file.findDimension("time") == null;
    Dimension dim = file.findDimension("date");
    assert dim != null;
    assert dim.getName().equals("date");
    assert dim.equals(d);
    assert dim == d;

    file.close();
  }

  public void testRewriteHeader() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.openExisting(filename, true);
    file.setRedefineMode(true);

    file.addGlobalAttribute("att8", "1234567");

    /////////////////////////////////////////////////
    boolean rewriteAll = file.setRedefineMode(false);
    assert !rewriteAll;

    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("1234567") : att.getStringValue();

    file.close();
  }

  public void testRewriteHeader2() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.openExisting(filename, true);
    file.setRedefineMode(true);

    file.addGlobalAttribute("att8", "123456789");

    /////////////////////////////////////////////////
    boolean rewriteAll = file.setRedefineMode(false);
    assert rewriteAll;


    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("123456789") : att.getStringValue();

    file.close();
  }

  public void testRewriteHeader3() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.createNew(filename2, true);
    file.addGlobalAttribute("att8", "1234567890");
    file.setExtraHeaderBytes(10);
    file.create();

    file.setRedefineMode(true);
    file.addGlobalAttribute("att8", "123456789012345");
    boolean rewriteAll = file.setRedefineMode(false);
    assert !rewriteAll;

    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("123456789012345") : att.getStringValue();

    file.close();
  }

  public void testRedefine3() throws IOException, InvalidRangeException {
    String filename = TestAll.temporaryLocalDataDir + "testRedefine3.nc";
    NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew (filename, false);
    ncFile.setExtraHeaderBytes (64*1000);
    Dimension dim = ncFile.addDimension ("time", 100);

    double[] jackData = new double[100];
    for (int i = 0; i < 100; i++) jackData[i] = i;
    double[] jillData = new double[100];
    for (int i = 0; i < 100; i++) jillData[i] = 2*i;

    Dimension[] dims = new Dimension[] {dim};
    ncFile.addVariable ("jack", DataType.DOUBLE, dims);
    ncFile.addVariableAttribute ("jack", "where", "up the hill");
    ncFile.create();

    int[] start = new int[] {0};
    int[] count = new int[] {100};
    ncFile.write ("jack", start, Array.factory (double.class, count, jackData));

    ncFile.setRedefineMode (true);
    ncFile.addVariable ("jill", DataType.DOUBLE, dims);
    ncFile.addVariableAttribute ("jill", "where", "up the hill");
    ncFile.setRedefineMode (false);

    Array jillArray = Array.factory (double.class, count, jillData);
    ncFile.write ("jill", start, jillArray);

    ncFile.flush();
    ncFile.close();

    NetcdfFile nc = NetcdfFile.open(filename, null);
    Variable v = nc.findVariable("jill");
    Array jillRead = v.read();
    CompareNetcdf.compareData(jillArray, jillRead);

    nc.close();
  }
}
